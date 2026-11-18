package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.dto.vehicle.Vehicle;

import java.util.List;

/**
 * 车主车辆 Mapper（成员 2 scaffold：最小可用实现，正式实现由成员 2 负责）。
 */
@Mapper
public interface VehicleMapper {

    List<Vehicle> selectByOwner(@Param("ownerUserId") Long ownerUserId);

    int insert(Vehicle vehicle);

    /** 按车牌取 ACTIVE 车辆（入场绑定用；未绑定/停用返回 null，作为临时车） */
    Vehicle selectActiveByPlate(@Param("licensePlate") String licensePlate);

    Vehicle selectByIdAndOwner(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);

    /** 不带归属过滤，用于区分“不存在(404)”与“非本人(403)” */
    Vehicle selectById(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId, @Param("status") String status);
}
