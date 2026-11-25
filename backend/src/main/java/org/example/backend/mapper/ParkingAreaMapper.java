package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.ParkingArea;

import java.util.List;

/**
 * 停车区域数据访问。SQL 全部写在 resources/mapper/ParkingAreaMapper.xml。
 *
 * <p>行锁说明：区域新增与状态变更都先对所属停车场行 {@code SELECT ... FOR UPDATE}，
 * 与入场、车位、收费规则切换保持同一加锁顺序（停车场 → 区域 → 车位）。
 * 成员 1 的 {@code ParkingLotGuard} 合并后，可用它替换本接口中的两个 lockLot 查询，
 * 业务代码与加锁顺序不变。</p>
 */
@Mapper
public interface ParkingAreaMapper {

    /** 按停车场查询区域，status 为 null 时不过滤；按 areaCode 升序。 */
    List<ParkingArea> selectByLot(@Param("lotId") Long lotId, @Param("status") String status);

    ParkingArea selectById(@Param("id") Long id);

    /** 同一停车场内区域编号是否已存在。 */
    int countByLotAndCode(@Param("lotId") Long lotId, @Param("areaCode") String areaCode);

    int insert(ParkingArea area);

    /** 只更新名称和说明，不动 areaCode。 */
    int updateInfo(ParkingArea area);

    /** 条件更新：只有当前状态等于 expectedStatus 才改成 status，返回 1 表示成功。 */
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("expectedStatus") String expectedStatus);

    int deleteById(@Param("id") Long id);

    /** 该区域下的车位数，用于「有车位不能删」判断。 */
    int countSpacesByArea(@Param("areaId") Long areaId);

    /** 该区域下处于「停车中」的记录数，用于「有在场车辆不能停用」判断。 */
    int countParkingRecordsByArea(@Param("areaId") Long areaId);

    /** 锁定停车场行并返回状态；返回 null 表示停车场不存在（必须先锁再读，不能先查后改）。 */
    String lockLotStatus(@Param("lotId") Long lotId);

    /** 按区域锁定其所属停车场行并返回停车场状态。 */
    String lockLotStatusByAreaId(@Param("areaId") Long areaId);
}
