package org.example.backend.dto;

/**
 * 入场选位结果：只包含返回给成员 3 需要的区域/车位展示字段。
 *
 * <p>由 {@code ParkingEntryMapper.selectFreeSpaceForUpdate} 在同一个停车场内按
 * {@code area_code, space_code, s.id} 固定排序选出，SQL 自带行锁。</p>
 */
public class ParkingSpaceAllocation {

    private Long id;
    private Long areaId;
    private String areaCode;
    private String spaceCode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getSpaceCode() {
        return spaceCode;
    }

    public void setSpaceCode(String spaceCode) {
        this.spaceCode = spaceCode;
    }
}
