package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.BusinessException;
import org.example.backend.common.CurrentUser;
import org.example.backend.common.ErrorCodes;
import org.example.backend.common.Plates;
import org.example.backend.dto.record.PaymentInfoDto;
import org.example.backend.dto.record.RecordDto;
import org.example.backend.dto.record.RecordFilter;
import org.example.backend.dto.record.RecordRow;
import org.example.backend.mapper.ParkingRecordQueryMapper;
import org.example.backend.service.fee.FeeCalculator;
import org.example.backend.service.fee.FeeRuleQuery;
import org.example.backend.service.fee.FeeRuleSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * 停车记录查询（成员 3，M5-01/02）。
 * 管理员与车主使用同一可见 DTO；车主列表/详情必须 JOIN vehicle 的 owner_user_id = 当前会话用户。
 */
@Service
@RequiredArgsConstructor
public class ParkingRecordQueryService {

    private static final Set<String> RECORD_STATUSES = Set.of("PARKING", "COMPLETED", "ABNORMAL");

    private final ParkingRecordQueryMapper recordQueryMapper;
    private final FeeRuleQuery feeRuleQuery;
    private final CurrentUser currentUser;
    private final Clock clock;

    /** 管理员列表：过滤项均可省；from/to 同时提供；按 entryTime DESC, id DESC */
    @Transactional(readOnly = true)
    public List<RecordDto> listForAdmin(RecordFilter filter) {
        validateFilter(filter);
        return recordQueryMapper.selectForAdmin(filter).stream()
                .map(this::toDto)
                .toList();
    }

    /** 管理员详情：不存在 404/4601 */
    @Transactional(readOnly = true)
    public RecordDto getForAdmin(Long id) {
        RecordRow row = recordQueryMapper.selectByIdForAdmin(id);
        if (row == null) {
            throw BusinessException.notFound(ErrorCodes.RECORD_NOT_FOUND, "停车记录不存在");
        }
        return toDto(row);
    }

    /** 车主列表：只返回本人绑定车辆关联的记录 */
    @Transactional(readOnly = true)
    public List<RecordDto> listForOwner(String status) {
        Long ownerUserId = currentUser.id();
        String normalizedStatus = normalizeStatus(status);
        return recordQueryMapper.selectForOwner(ownerUserId, normalizedStatus).stream()
                .map(this::toDto)
                .toList();
    }

    /** 车主详情：非本人记录 403/4602；不存在 404/4601 */
    @Transactional(readOnly = true)
    public RecordDto getForOwner(Long id) {
        Long ownerUserId = currentUser.id();
        RecordRow row = recordQueryMapper.selectByIdForOwner(id, ownerUserId);
        if (row == null) {
            boolean exists = recordQueryMapper.selectByIdForAdmin(id) != null;
            if (exists) {
                throw BusinessException.forbidden(ErrorCodes.RECORD_NOT_OWNED, "不能查看他人的停车记录");
            }
            throw BusinessException.notFound(ErrorCodes.RECORD_NOT_FOUND, "停车记录不存在");
        }
        return toDto(row);
    }

    private void validateFilter(RecordFilter filter) {
        if (filter.getLicensePlate() != null) {
            if (filter.getLicensePlate().isBlank()) {
                throw BusinessException.badRequest(ErrorCodes.BAD_REQUEST, "过滤值不能为空字符串");
            }
            filter.setLicensePlate(Plates.normalize(filter.getLicensePlate()));
        }
        if (filter.getStatus() != null) {
            if (filter.getStatus().isBlank()) {
                throw BusinessException.badRequest(ErrorCodes.BAD_REQUEST, "过滤值不能为空字符串");
            }
            normalizeStatus(filter.getStatus());
        }
        OffsetDateTime from = filter.getEntryFrom();
        OffsetDateTime to = filter.getEntryTo();
        if ((from == null) != (to == null)) {
            throw BusinessException.badRequest(ErrorCodes.BAD_REQUEST, "from 与 to 必须同时提供");
        }
        if (from != null && !from.isBefore(to)) {
            throw BusinessException.badRequest(ErrorCodes.DATE_RANGE_INVALID, "日期范围错误：from 必须早于 to");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        if (status.isBlank()) {
            throw BusinessException.badRequest(ErrorCodes.BAD_REQUEST, "过滤值不能为空字符串");
        }
        String normalized = status.trim().toUpperCase();
        if (!RECORD_STATUSES.contains(normalized)) {
            throw BusinessException.badRequest(ErrorCodes.BAD_REQUEST, "状态过滤值仅支持 PARKING/COMPLETED/ABNORMAL");
        }
        return normalized;
    }

    private RecordDto toDto(RecordRow row) {
        RecordDto dto = new RecordDto();
        dto.setId(row.getId());
        dto.setRecordNo(row.getRecordNo());
        dto.setLotId(row.getParkingLotId());
        dto.setLotName(row.getLotName());
        dto.setVehicleId(row.getVehicleId());
        dto.setLicensePlate(row.getLicensePlate());
        dto.setSpaceId(row.getParkingSpaceId());
        dto.setAreaCode(row.getAreaCode());
        dto.setSpaceCode(row.getSpaceCode());
        dto.setFeeRuleId(row.getFeeRuleId());
        dto.setEntryTime(row.getEntryTime());
        dto.setExitTime(row.getExitTime());
        dto.setStatus(row.getStatus());
        dto.setAbnormalReason(row.getAbnormalReason());

        OffsetDateTime end = row.getExitTime() != null ? row.getExitTime() : OffsetDateTime.now(clock);
        FeeRuleSnapshot rule = feeRuleQuery.findSnapshotByIdOrNull(row.getFeeRuleId());
        if (rule == null) {
            // 规则行缺失（异常数据）时仍给时长，不给预估金额
            dto.setDurationMinutes(FeeCalculator.ceilMinutes(row.getEntryTime(), end));
        } else {
            FeeCalculator.FeeResult result = FeeCalculator.calculate(row.getEntryTime(), end, rule);
            dto.setDurationMinutes(result.durationMinutes());
            if ("PARKING".equals(row.getStatus())) {
                dto.setEstimatedAmountDue(result.amountDue());
            }
        }

        if ("PARKING".equals(row.getStatus())) {
            dto.setPaymentStatus("UNPAID");
            dto.setPayment(null);
        } else if (row.getPaymentNo() != null) {
            dto.setPayment(new PaymentInfoDto(row.getPaymentNo(), row.getAmountDue(),
                    row.getAmountPaid(), row.getPaymentMethod(), row.getPaymentTime()));
            dto.setPaymentStatus("FREE_RELEASE".equals(row.getPaymentMethod()) ? "WAIVED" : "PAID");
        } else {
            // ABNORMAL：无支付记录，不计收入
            dto.setPaymentStatus("ABNORMAL".equals(row.getStatus()) ? "ABNORMAL" : null);
            dto.setPayment(null);
        }
        return dto;
    }
}
