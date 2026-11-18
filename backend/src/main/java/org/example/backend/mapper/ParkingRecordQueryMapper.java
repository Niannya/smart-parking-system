package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.dto.record.RecordFilter;
import org.example.backend.dto.record.RecordRow;

import java.util.List;

/**
 * 停车记录查询 Mapper（成员 3，独立 XML：ParkingRecordQueryMapper.xml）。
 * 车主查询必须 JOIN vehicle ON record.vehicle_id = vehicle.id AND vehicle.owner_user_id = 当前会话用户，
 * 不能仅凭车牌快照，也不接受客户端提供的 owner ID。
 */
@Mapper
public interface ParkingRecordQueryMapper {

    /** 管理员列表：动态过滤，entryTime DESC, id DESC */
    List<RecordRow> selectForAdmin(RecordFilter filter);

    RecordRow selectByIdForAdmin(@Param("id") Long id);

    /** 车主列表：只返回本人绑定车辆关联的记录 */
    List<RecordRow> selectForOwner(@Param("ownerUserId") Long ownerUserId,
                                   @Param("status") String status);

    RecordRow selectByIdForOwner(@Param("id") Long id,
                                 @Param("ownerUserId") Long ownerUserId);
}
