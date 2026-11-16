package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.dto.ParkingLotOption;
import org.example.backend.entity.ParkingArea;

import java.util.List;

/**
 * 停车区域数据访问。SQL 全部写在 resources/mapper/ParkingAreaMapper.xml。
 */
@Mapper
public interface ParkingAreaMapper {

    /** 按停车场查询区域，status 为 null 时不过滤 */
    List<ParkingArea> selectByLot(@Param("lotId") Long lotId, @Param("status") String status);

    ParkingArea selectById(@Param("id") Long id);

    /** 同一停车场内区域编号是否已存在 */
    int countByLotAndCode(@Param("lotId") Long lotId, @Param("areaCode") String areaCode);

    int insert(ParkingArea area);

    /** 只更新名称和说明，不动 areaCode */
    int updateInfo(ParkingArea area);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int deleteById(@Param("id") Long id);

    /** 该区域下的车位数，用于「有车位不能删」判断 */
    int countSpacesByArea(@Param("areaId") Long areaId);

    /** 该区域下处于「停车中」的记录数，用于「有在场车辆不能停用」判断 */
    int countParkingRecordsByArea(@Param("areaId") Long areaId);

    /** 查询停车场状态：返回 null 表示停车场不存在 */
    String selectLotStatus(@Param("lotId") Long lotId);

    /** 启用状态的停车场下拉选项（临时接口，等成员 1 的停车场模块合并后删除） */
    List<ParkingLotOption> selectActiveLots();
}
