package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.dto.entry.ParkingRecordInsert;

/**
 * 入场 Mapper（成员 2 scaffold：独立 XML ParkingEntryMapper.xml，与离场不共用）。
 */
@Mapper
public interface ParkingEntryMapper {

    /** 全局同牌在场检查（唯一索引 uk_parking_record_active_plate 是全局的） */
    int countActiveByPlate(@Param("licensePlate") String licensePlate);

    boolean recordNoExists(@Param("recordNo") String recordNo);

    int insert(ParkingRecordInsert record);
}
