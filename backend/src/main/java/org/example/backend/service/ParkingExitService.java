package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.common.BusinessException;
import org.example.backend.common.CurrentUser;
import org.example.backend.common.ErrorCodes;
import org.example.backend.common.Plates;
import org.example.backend.dto.exit.ActiveRecordDto;
import org.example.backend.dto.exit.ExitRequest;
import org.example.backend.dto.exit.ExitResultDto;
import org.example.backend.dto.exit.ParkingRecordRow;
import org.example.backend.dto.exit.PaymentInsert;
import org.example.backend.dto.exit.QuoteDto;
import org.example.backend.mapper.ParkingExitMapper;
import org.example.backend.service.fee.FeeCalculator;
import org.example.backend.service.fee.FeeRuleQuery;
import org.example.backend.service.fee.FeeRuleSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 离场与模拟支付（成员 3，M4-01/02/03）。
 *
 * 正常离场一个事务：锁在场记录 → 读取其绑定的规则/车位 → 计算 → 校验支付 →
 * 生成唯一 paymentNo → 插 payment_record → 条件更新记录 COMPLETED 并写 exitTime/exitAdminId →
 * 条件释放 OCCUPIED→FREE；任何一步行数不为 1 抛 4505 并回滚。
 * 重复 POST 为 409/4502，不返回第二次成功。
 *
 * 异常离场：原因必填，锁记录并在同一事务设置 ABNORMAL、abnormalReason、exitTime、exitAdminId
 * 并释放车位，不生成支付记录，默认不计收入；不得当作免费放行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingExitService {

    private final ParkingExitMapper exitMapper;
    private final FeeRuleQuery feeRuleQuery;
    private final CurrentUser currentUser;
    private final Clock clock;

    /** GET /api/parking-exits/active：车牌或记录编号恰填一个；无则 404/4501 */
    @Transactional(readOnly = true)
    public ActiveRecordDto findActive(String licensePlate, String recordNo) {
        boolean hasPlate = licensePlate != null && !licensePlate.isBlank();
        boolean hasRecordNo = recordNo != null && !recordNo.isBlank();
        if (hasPlate == hasRecordNo) {
            throw BusinessException.badRequest(ErrorCodes.BAD_REQUEST, "车牌与记录编号必须且只能填写一个");
        }
        ParkingRecordRow record = hasPlate
                ? exitMapper.selectActiveByPlate(Plates.normalize(licensePlate))
                : exitMapper.selectActiveByRecordNo(recordNo.trim());
        if (record == null) {
            throw BusinessException.notFound(ErrorCodes.EXIT_NO_ACTIVE_RECORD, "没有对应的在场停车记录");
        }
        return toActiveDto(record);
    }

    /** GET /api/parking-exits/{recordId}/quote：只接受 PARKING；读取记录绑定的规则，不取场当前规则 */
    @Transactional(readOnly = true)
    public QuoteDto quote(Long recordId) {
        ParkingRecordRow record = exitMapper.selectById(recordId);
        if (record == null) {
            throw BusinessException.notFound(ErrorCodes.EXIT_NO_ACTIVE_RECORD, "停车记录不存在");
        }
        if (!"PARKING".equals(record.getStatus())) {
            throw BusinessException.conflict(ErrorCodes.EXIT_ALREADY_COMPLETED, "该记录已离场，无法预览费用");
        }
        FeeRuleSnapshot rule = feeRuleQuery.getSnapshotById(record.getFeeRuleId());
        OffsetDateTime quotedExitTime = OffsetDateTime.now(clock);
        FeeCalculator.FeeResult result = FeeCalculator.calculate(record.getEntryTime(), quotedExitTime, rule);
        return new QuoteDto(record.getId(), record.getEntryTime(), quotedExitTime,
                result.durationMinutes(), result.amountDue(), rule.feeRuleId());
    }

    /** POST /api/parking-exits/{recordId}：正常离场 + 模拟确认收款 */
    @Transactional(rollbackFor = Exception.class)
    public ExitResultDto exit(Long recordId, ExitRequest request) {
        ParkingRecordRow record = exitMapper.selectByIdForUpdate(recordId);
        if (record == null) {
            throw BusinessException.notFound(ErrorCodes.EXIT_NO_ACTIVE_RECORD, "停车记录不存在");
        }
        if (!"PARKING".equals(record.getStatus())) {
            throw BusinessException.conflict(ErrorCodes.EXIT_ALREADY_COMPLETED, "该记录已离场，不能重复支付");
        }

        // 读取记录绑定的规则（入场时固定），不取停车场后来启用的新规则
        FeeRuleSnapshot rule = feeRuleQuery.getSnapshotById(record.getFeeRuleId());
        OffsetDateTime exitTime = OffsetDateTime.now(clock);
        FeeCalculator.FeeResult result = FeeCalculator.calculate(record.getEntryTime(), exitTime, rule);

        BigDecimal amountPaid = validatePayment(request, result.amountDue());

        String paymentNo = generatePaymentNo();
        PaymentInsert payment = new PaymentInsert();
        payment.setPaymentNo(paymentNo);
        payment.setParkingRecordId(record.getId());
        payment.setAmountDue(result.amountDue());
        payment.setAmountPaid(amountPaid);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentTime(exitTime);
        payment.setRemark(trimToNull(request.getRemark()));
        payment.setHandledBy(currentUser.requireAdminId());
        exitMapper.insertPayment(payment);

        int completed = exitMapper.completeRecord(record.getId(), exitTime, currentUser.requireAdminId());
        if (completed != 1) {
            throw BusinessException.conflict(ErrorCodes.EXIT_CONCURRENT_CONFLICT, "离场并发冲突，请刷新后重试");
        }
        int released = exitMapper.releaseSpace(record.getParkingSpaceId());
        if (released != 1) {
            throw BusinessException.conflict(ErrorCodes.EXIT_CONCURRENT_CONFLICT, "车位释放并发冲突，请刷新后重试");
        }

        return new ExitResultDto(record.getId(), paymentNo, exitTime, result.durationMinutes(),
                result.amountDue(), amountPaid, "COMPLETED", "FREE");
    }

    /** POST /api/parking-exits/{recordId}/abnormal：人工异常处置通道，不生成支付记录 */
    @Transactional(rollbackFor = Exception.class)
    public ExitResultDto abnormal(Long recordId, org.example.backend.dto.exit.AbnormalExitRequest request) {
        ParkingRecordRow record = exitMapper.selectByIdForUpdate(recordId);
        if (record == null) {
            throw BusinessException.notFound(ErrorCodes.EXIT_NO_ACTIVE_RECORD, "停车记录不存在");
        }
        if (!"PARKING".equals(record.getStatus())) {
            throw BusinessException.conflict(ErrorCodes.EXIT_ALREADY_COMPLETED, "该记录已离场，不能重复操作");
        }
        String reason = request.getReason() == null ? null : request.getReason().trim();
        if (reason == null || reason.isEmpty()) {
            throw BusinessException.badRequest(ErrorCodes.BAD_REQUEST, "异常离场原因必填");
        }

        FeeRuleSnapshot rule = feeRuleQuery.getSnapshotById(record.getFeeRuleId());
        OffsetDateTime exitTime = OffsetDateTime.now(clock);
        FeeCalculator.FeeResult result = FeeCalculator.calculate(record.getEntryTime(), exitTime, rule);

        int marked = exitMapper.markAbnormal(record.getId(), reason, exitTime, currentUser.requireAdminId());
        if (marked != 1) {
            throw BusinessException.conflict(ErrorCodes.EXIT_CONCURRENT_CONFLICT, "离场并发冲突，请刷新后重试");
        }
        int released = exitMapper.releaseSpace(record.getParkingSpaceId());
        if (released != 1) {
            throw BusinessException.conflict(ErrorCodes.EXIT_CONCURRENT_CONFLICT, "车位释放并发冲突，请刷新后重试");
        }

        // 不插 payment_record；amountDue 仅作核对参考，amountPaid 为 null，默认不计收入
        return new ExitResultDto(record.getId(), null, exitTime, result.durationMinutes(),
                result.amountDue(), null, "ABNORMAL", "FREE");
    }

    /** 支付校验：普通支付实收=应收；免费放行实收 0 且原因必填 */
    private BigDecimal validatePayment(ExitRequest request, BigDecimal amountDue) {
        String method = request.getPaymentMethod();
        if ("FREE_RELEASE".equals(method)) {
            BigDecimal paid = request.getAmountPaid() == null ? BigDecimal.ZERO : request.getAmountPaid();
            if (paid.compareTo(BigDecimal.ZERO) != 0) {
                throw BusinessException.conflict(ErrorCodes.EXIT_AMOUNT_MISMATCH, "免费放行实收金额必须为 0");
            }
            String remark = request.getRemark() == null ? null : request.getRemark().trim();
            if (remark == null || remark.isEmpty()) {
                throw BusinessException.conflict(ErrorCodes.EXIT_FREE_RELEASE_REASON_REQUIRED, "免费放行必须填写原因");
            }
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal paid = request.getAmountPaid();
        if (paid == null) {
            throw BusinessException.badRequest(ErrorCodes.BAD_REQUEST, "实收金额不能为空");
        }
        if (paid.compareTo(amountDue) != 0) {
            throw BusinessException.conflict(ErrorCodes.EXIT_AMOUNT_MISMATCH,
                    "实收金额与服务端应收额不一致：应收 " + amountDue.toPlainString());
        }
        return paid.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** 支付编号：P + UUID 去连字符后前 31 位（共 32 字符），预检唯一，兜底靠唯一索引 */
    private String generatePaymentNo() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String paymentNo = "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 31);
            if (!exitMapper.paymentNoExists(paymentNo)) {
                return paymentNo;
            }
        }
        throw BusinessException.conflict(ErrorCodes.EXIT_CONCURRENT_CONFLICT, "支付编号生成冲突，请重试");
    }

    private ActiveRecordDto toActiveDto(ParkingRecordRow record) {
        ActiveRecordDto dto = new ActiveRecordDto();
        dto.setId(record.getId());
        dto.setRecordNo(record.getRecordNo());
        dto.setLotId(record.getParkingLotId());
        dto.setLicensePlate(record.getLicensePlate());
        dto.setSpaceId(record.getParkingSpaceId());
        dto.setAreaCode(record.getAreaCode());
        dto.setSpaceCode(record.getSpaceCode());
        dto.setEntryTime(record.getEntryTime());
        dto.setStatus(record.getStatus());
        return dto;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
