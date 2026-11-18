package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.dto.lot.ParkingLotOption;

import java.util.List;

/**
 * 停车场只读 Mapper（成员 1  scaffold：供 ParkingLotGuard 行锁与选项查询使用）。
 */
@Mapper
public interface ParkingLotMapper {

    /** SELECT 1 FROM parking_lot WHERE id = #{lotId} FOR UPDATE；不存在返回 null（不可用 primitive，否则 MyBatis 报 BindingException） */
    Integer lockById(@Param("lotId") Long lotId);

    String selectStatusById(@Param("lotId") Long lotId);

    /** 场地选项：只取 ACTIVE，按 lotCode 升序（01 契约：GET /api/parking-lots/options） */
    List<ParkingLotOption> selectOptions();
}
