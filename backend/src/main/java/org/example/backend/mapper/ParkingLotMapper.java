package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.dto.ParkingLotDto;

import java.util.List;

@Mapper
public interface ParkingLotMapper {

    List<ParkingLotDto.View> selectLots(@Param("status") String status);

    List<ParkingLotDto.Option> selectActiveOptions();

    ParkingLotDto.View selectById(@Param("id") Long id);

    ParkingLotDto.View selectByCode(@Param("lotCode") String lotCode);

    int insert(@Param("lotCode") String lotCode, @Param("lotName") String lotName,
               @Param("address") String address, @Param("description") String description);

    int updateInfo(@Param("id") Long id, @Param("lotName") String lotName,
                   @Param("address") String address, @Param("description") String description);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    boolean hasActiveRecords(@Param("id") Long id);

    boolean hasDependencies(@Param("id") Long id);

    int deleteById(@Param("id") Long id);
}
