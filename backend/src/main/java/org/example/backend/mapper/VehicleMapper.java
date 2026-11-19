package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.Vehicle;

import java.util.List;

/**
 * 车主车辆数据访问。SQL 全部写在 resources/mapper/VehicleMapper.xml。
 *
 * <p>三条硬规矩：列表/启停都带 {@code owner_user_id} 条件；归属只来自会话；
 * 不提供物理删除与换绑。</p>
 */
@Mapper
public interface VehicleMapper {

    /** 只查询本人车辆，按车牌升序。 */
    List<Vehicle> selectByOwner(@Param("ownerUserId") Long ownerUserId);

    /** 按 ID 查询（用于区分「不存在」与「不是本人车辆」，不返回归属账号）。 */
    Vehicle selectById(@Param("id") Long id);

    int insert(Vehicle vehicle);

    /** 带归属过滤的启停；返回 0 表示不存在或不是本人。 */
    int updateStatus(@Param("id") Long id,
                     @Param("ownerUserId") Long ownerUserId,
                     @Param("status") String status);
}
