package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.ParkingArea;
import org.example.backend.entity.ParkingSpace;

import java.util.List;

/**
 * 停车位数据访问。SQL 全部写在 resources/mapper/ParkingSpaceMapper.xml。
 *
 * <p>归属校验一律通过 {@code parking_space -> parking_area -> parking_lot} JOIN 反查，
 * 不信任请求体里的 lotId。状态变更使用条件 UPDATE（带 expectedStatus），
 * 避免「先查后改」之间状态被入场/离场事务改掉。</p>
 */
@Mapper
public interface ParkingSpaceMapper {

    /** 按停车场（必填）、区域、状态查询；按区域编号、车位编号升序。 */
    List<ParkingSpace> selectByLot(@Param("lotId") Long lotId,
                                   @Param("areaId") Long areaId,
                                   @Param("status") String status);

    ParkingSpace selectById(@Param("id") Long id);

    /** 同一区域内车位编号是否已存在。 */
    int countByAreaAndCode(@Param("areaId") Long areaId, @Param("spaceCode") String spaceCode);

    /** 该车位被多少条停车记录引用（任意状态），用于「有历史记录不可改编号/删除」。 */
    int countRecordsBySpace(@Param("spaceId") Long spaceId);

    int insert(ParkingSpace space);

    int updateInfo(@Param("id") Long id,
                   @Param("spaceCode") String spaceCode,
                   @Param("description") String description);

    /** 条件更新：只有当前状态等于 expectedStatus 才改成 status。 */
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("expectedStatus") String expectedStatus);

    int deleteById(@Param("id") Long id);

    /** 读取区域基本信息（id、所属停车场、编号、状态），用于归属与启用状态校验。 */
    ParkingArea selectAreaById(@Param("areaId") Long areaId);

    /** 锁定停车场行并返回状态；返回 null 表示不存在。 */
    String lockLotStatus(@Param("lotId") Long lotId);

    /** 按车位锁定其所属停车场行并返回停车场状态。 */
    String lockLotStatusBySpaceId(@Param("spaceId") Long spaceId);
}
