package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.dto.area.ParkingArea;

import java.util.List;

/**
 * 停车区域 Mapper（成员 2 scaffold：最小可用实现，正式实现由成员 2 负责）。
 */
@Mapper
public interface ParkingAreaMapper {

    List<ParkingArea> selectByLot(@Param("lotId") Long lotId);

    int insert(ParkingArea area);

    /** 同场编号唯一校验用 */
    int countByLotAndCode(@Param("lotId") Long lotId, @Param("areaCode") String areaCode);

    /** 车位/入场归属校验：区域所属停车场；不存在返回 null */
    Long selectLotIdByArea(@Param("areaId") Long areaId);

    String selectStatusById(@Param("areaId") Long areaId);
}
