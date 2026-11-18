package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ParkingLotGuardMapper {

    String selectStatusForUpdate(@Param("lotId") Long lotId);
}
