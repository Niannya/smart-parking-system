package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCode;
import org.example.backend.dto.ParkingEntryRequest;
import org.example.backend.dto.ParkingEntryResult;
import org.example.backend.dto.ParkingSpaceAllocation;
import org.example.backend.entity.ParkingRecord;
import org.example.backend.mapper.ParkingEntryMapper;
import org.example.backend.support.CurrentUserProvider;
import org.example.backend.support.CurrentUserView;
import org.example.backend.support.PlateNormalizer;
import org.example.backend.support.RequestValues;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 车辆入场业务（M3-01/02/03）。
 *
 * <p>顺序固定为：</p>
 * <ol>
 *   <li>拒绝未知字段、规范化并校验车牌（4204）；</li>
 *   <li>当前会话必须是 ADMIN（1003/1004）；</li>
 *   <li>事务内锁启用停车场（成员 1 {@code ParkingLotGuard}，本模块只通过
 *       {@link ParkingEntryDependency} 调用）；</li>
 *   <li>全局同车牌 PARKING 检查（4301）；</li>
 *   <li>取该场生效规则（成员 3 {@code FeeRuleQuery}，无规则 4303）；</li>
 *   <li>只取启用区域的 FREE 车位，按 {@code area_code,space_code,id} 固定排序，
 *       SQL 带 {@code FOR UPDATE SKIP LOCKED}；</li>
 *   <li>条件占用车位（4304），插入停车记录；任一步失败整体回滚，车位保持 FREE；</li>
 *   <li>启用的绑定车辆写 vehicleId，停用或未绑定写 null，车牌快照始终保存。</li>
 * </ol>
 *
 * <p>成员 1 的 Guard 已通过 {@link ParkingEntryDependencyAdapter} 接入；成员 3 的
 * {@code FeeRuleQuery} 尚未合并时，取规则方法会抛出明确的 500 业务错误，
 * 入场接口不会伪装成功。替换方式见 {@code docs/成员2_交付与合并操作说明.md}。</p>
 */
@Service
public class ParkingEntryService {

    private static final String PARKING = "PARKING";
    private static final ZoneId API_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int RECORD_NO_MAX_ATTEMPTS = 3;

    private final ParkingEntryMapper parkingEntryMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ParkingEntryDependency parkingEntryDependency;

    public ParkingEntryService(ParkingEntryMapper parkingEntryMapper,
                               CurrentUserProvider currentUserProvider,
                               ParkingEntryDependency parkingEntryDependency) {
        this.parkingEntryMapper = parkingEntryMapper;
        this.currentUserProvider = currentUserProvider;
        this.parkingEntryDependency = parkingEntryDependency;
    }

    @Transactional(rollbackFor = Exception.class)
    public ParkingEntryResult enter(ParkingEntryRequest request) {
        request.rejectUnknownFields();

        // 1. 车牌校验先于权限与数据库访问（契约校验顺序）
        String plate = PlateNormalizer.normalize(request.getLicensePlate());
        if (plate.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_LICENSE_PLATE, "车牌不能为空");
        }
        if (!PlateNormalizer.isValid(plate)) {
            throw new BusinessException(ErrorCode.INVALID_LICENSE_PLATE, "车牌格式不正确，示例：京A12345");
        }

        CurrentUserView admin = requireAdmin();
        Long lotId = RequestValues.requirePositive(request.getLotId(), "lotId 必填，且必须为正整数");

        // 2. 同一事务的第一条：成员 1 的 Guard 锁停车场行并校验 ACTIVE
        parkingEntryDependency.lockActiveLot(lotId);

        // 3. 全局同牌检查（跨场也不允许同牌同时在场）
        if (parkingEntryMapper.countActiveByPlate(plate) > 0) {
            throw new BusinessException(ErrorCode.PLATE_ALREADY_PARKING, "该车牌已在场，请先办理离场");
        }

        // 4. 成员 3 的规则查询只读返回当前生效规则 ID；无规则不允许入场
        Long feeRuleId = parkingEntryDependency.findActiveFeeRuleId(lotId);
        if (feeRuleId == null) {
            throw new BusinessException(ErrorCode.NO_ACTIVE_FEE_RULE, "当前停车场无生效收费规则");
        }

        // 5. 固定排序 + 行锁选一个启用区域的 FREE 车位
        ParkingSpaceAllocation space = parkingEntryMapper.selectFreeSpaceForUpdate(lotId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NO_FREE_SPACE, "当前停车场无空闲车位");
        }

        // 6. 条件占位；拿不到说明被并发请求抢先，整个事务回滚
        if (parkingEntryMapper.occupySpace(space.getId()) != 1) {
            throw new BusinessException(ErrorCode.ENTRY_CONFLICT, "入场并发冲突，请重新提交");
        }

        // 7. 启用的绑定车辆写 vehicleId，否则临时车写 null
        Long vehicleId = parkingEntryMapper.selectActiveVehicleIdByPlate(plate);

        ParkingRecord record = new ParkingRecord();
        record.setParkingLotId(lotId);
        record.setVehicleId(vehicleId);
        record.setLicensePlate(plate);
        record.setParkingSpaceId(space.getId());
        record.setFeeRuleId(feeRuleId);
        record.setEntryAdminId(admin.id());
        record.setEntryTime(OffsetDateTime.now(API_ZONE));
        record.setStatus(PARKING);

        insertRecordWithUniqueRecordNo(record);

        return toResult(record, space);
    }

    /**
     * 插入停车记录；record_no 极罕见碰撞时在同一事务内重新生成。
     *
     * <p>PostgreSQL 中一条语句报唯一冲突后事务会进入 aborted 状态，因此不能用
     * “catch 后再查库”的方式判断；record_no 冲突用 {@code ON CONFLICT DO NOTHING}
     * 变成 0 行而不中断事务，其他唯一冲突才抛异常。</p>
     */
    private void insertRecordWithUniqueRecordNo(ParkingRecord record) {
        for (int attempt = 1; attempt <= RECORD_NO_MAX_ATTEMPTS; attempt++) {
            record.setRecordNo(newRecordNo());
            try {
                if (parkingEntryMapper.insertRecord(record) == 1) {
                    return;
                }
            } catch (DuplicateKeyException e) {
                throw translateDuplicateKey(e);
            }
        }
        throw new BusinessException(ErrorCode.ENTRY_CONFLICT, "记录编号生成冲突，请重新提交");
    }

    private BusinessException translateDuplicateKey(DuplicateKeyException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.contains("uk_parking_record_active_plate")) {
            return new BusinessException(ErrorCode.PLATE_ALREADY_PARKING, "该车牌已在场，请先办理离场");
        }
        if (message.contains("uk_parking_record_active_space")) {
            return new BusinessException(ErrorCode.ENTRY_CONFLICT, "入场并发冲突，请重新提交");
        }
        return new BusinessException(ErrorCode.ENTRY_CONFLICT, "入场并发冲突，请重新提交");
    }

    /** 约定：R + UUID 去连字符后的前 31 位，共 32 字符。 */
    static String newRecordNo() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "R" + uuid.substring(0, 31);
    }

    private ParkingEntryResult toResult(ParkingRecord record, ParkingSpaceAllocation space) {
        ParkingEntryResult result = new ParkingEntryResult();
        result.setId(record.getId());
        result.setRecordNo(record.getRecordNo());
        result.setLotId(record.getParkingLotId());
        result.setLicensePlate(record.getLicensePlate());
        result.setSpaceId(record.getParkingSpaceId());
        result.setAreaCode(space.getAreaCode());
        result.setSpaceCode(space.getSpaceCode());
        result.setFeeRuleId(record.getFeeRuleId());
        result.setEntryTime(record.getEntryTime());
        result.setStatus(record.getStatus());
        return result;
    }

    private CurrentUserView requireAdmin() {
        CurrentUserView user = currentUserProvider.currentUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "请先登录");
        }
        if (!user.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号无权执行该操作");
        }
        return user;
    }
}
