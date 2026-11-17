package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.dto.ParkingSpaceAllocation;
import org.example.backend.entity.ParkingRecord;

/**
 * 入场专用 Mapper（M3-01/02/03）。
 *
 * <p>只负责“选位、占位、查同牌、查绑定车辆、插入记录”；锁启用停车场和取活动规则
 * 由 {@link org.example.backend.service.ParkingEntryDependency} 委托成员 1/3，
 * 本 Mapper 不复制这两段逻辑。</p>
 *
 * <p>成员 3 的离场/查询使用独立的 {@code ParkingExitMapper} / {@code ParkingRecordQueryMapper}，
 * 不修改本文件。</p>
 */
@Mapper
public interface ParkingEntryMapper {

    int countActiveByPlate(@Param("licensePlate") String licensePlate);

    ParkingSpaceAllocation selectFreeSpaceForUpdate(@Param("lotId") Long lotId);

    int occupySpace(@Param("spaceId") Long spaceId);

    Long selectActiveVehicleIdByPlate(@Param("licensePlate") String licensePlate);

    int insertRecord(ParkingRecord record);
}
