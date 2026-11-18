package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.dto.space.ParkingSpace;

import java.util.List;

/**
 * 停车位 Mapper（成员 2 scaffold：最小可用实现，正式实现由成员 2 负责）。
 */
@Mapper
public interface ParkingSpaceMapper {

    /** 按 lotId（必填）/areaId/status 过滤，按区域码、车位码升序 */
    List<ParkingSpace> selectByFilter(@Param("lotId") Long lotId,
                                      @Param("areaId") Long areaId,
                                      @Param("status") String status);

    int insert(ParkingSpace space);

    /** 入场分配：启用场→启用区域→FREE 车位，按 areaCode, spaceCode, id 排序，行锁 */
    ParkingSpace selectFreeForAllocationForUpdate(@Param("lotId") Long lotId);

    /** 条件占用：WHERE id AND status='FREE'，返回受影响行数 */
    int occupyIfFree(@Param("id") Long id);
}
