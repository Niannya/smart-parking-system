package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.BusinessException;
import org.example.backend.common.CurrentUser;
import org.example.backend.common.ErrorCodes;
import org.example.backend.common.ParkingLotGuard;
import org.example.backend.common.Plates;
import org.example.backend.dto.entry.EntryResultDto;
import org.example.backend.dto.entry.ParkingEntryRequest;
import org.example.backend.dto.entry.ParkingRecordInsert;
import org.example.backend.dto.space.ParkingSpace;
import org.example.backend.dto.vehicle.Vehicle;
import org.example.backend.mapper.ParkingEntryMapper;
import org.example.backend.mapper.ParkingSpaceMapper;
import org.example.backend.mapper.VehicleMapper;
import org.example.backend.service.fee.FeeRuleQuery;
import org.example.backend.service.fee.FeeRuleSnapshot;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 自动分配与入场事务（成员 2 scaffold：最小可用实现，正式实现由成员 2 负责）。
 *
 * 一个事务：锁启用停车场 → 确认全局无同牌 PARKING → 取该场活动规则
 * （消费成员 3 的 FeeRuleQuery.findActiveForLot，无规则 4303）→
 * 仅在该场启用区域选 FREE 车位（areaCode, spaceCode, id 排序，行锁 + 条件占用）→
 * 插入停车记录（recordNo=R+31 位，vehicleId 仅绑定 ACTIVE 车辆时写入）。
 * SQL 唯一索引 uk_parking_record_active_plate / uk_parking_record_active_space 是并发兜底。
 */
@Service
@RequiredArgsConstructor
public class ParkingEntryService {

    private final ParkingLotGuard parkingLotGuard;
    private final FeeRuleQuery feeRuleQuery;
    private final ParkingSpaceMapper spaceMapper;
    private final ParkingEntryMapper entryMapper;
    private final VehicleMapper vehicleMapper;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Transactional(rollbackFor = Exception.class)
    public EntryResultDto entry(ParkingEntryRequest request) {
        String licensePlate = Plates.normalize(request.getLicensePlate());
        Long adminId = currentUser.requireAdminId();
        Long lotId = request.getLotId();

        parkingLotGuard.lockActiveLot(lotId);

        if (entryMapper.countActiveByPlate(licensePlate) > 0) {
            throw BusinessException.conflict(ErrorCodes.ENTRY_PLATE_ACTIVE, "该车牌已在场，不能重复入场");
        }

        FeeRuleSnapshot rule = feeRuleQuery.findActiveForLot(lotId);

        ParkingSpace space = spaceMapper.selectFreeForAllocationForUpdate(lotId);
        if (space == null) {
            throw BusinessException.conflict(ErrorCodes.ENTRY_NO_FREE_SPACE, "当前停车场没有空闲车位");
        }
        int occupied = spaceMapper.occupyIfFree(space.getId());
        if (occupied != 1) {
            throw BusinessException.conflict(ErrorCodes.ENTRY_CONCURRENT_CONFLICT, "入场并发冲突，请重试");
        }

        Vehicle vehicle = vehicleMapper.selectActiveByPlate(licensePlate);
        String recordNo = generateRecordNo();
        OffsetDateTime entryTime = OffsetDateTime.now(clock);

        ParkingRecordInsert record = new ParkingRecordInsert();
        record.setRecordNo(recordNo);
        record.setParkingLotId(lotId);
        record.setParkingSpaceId(space.getId());
        record.setFeeRuleId(rule.feeRuleId());
        record.setVehicleId(vehicle == null ? null : vehicle.getId());
        record.setLicensePlate(licensePlate);
        record.setEntryAdminId(adminId);
        record.setEntryTime(entryTime);
        try {
            entryMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 唯一索引兜底：同牌在场或同车位被并发占用
            throw BusinessException.conflict(ErrorCodes.ENTRY_CONCURRENT_CONFLICT, "入场并发冲突，请重试");
        }

        return new EntryResultDto(record.getId(), recordNo, lotId, licensePlate, space.getId(),
                space.getAreaCode(), space.getSpaceCode(), rule.feeRuleId(), entryTime, "PARKING");
    }

    /** 记录编号：R + UUID 去连字符后前 31 位（共 32 字符），预检唯一 */
    private String generateRecordNo() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String recordNo = "R" + UUID.randomUUID().toString().replace("-", "").substring(0, 31);
            if (!entryMapper.recordNoExists(recordNo)) {
                return recordNo;
            }
        }
        throw BusinessException.conflict(ErrorCodes.ENTRY_CONCURRENT_CONFLICT, "记录编号生成冲突，请重试");
    }
}
