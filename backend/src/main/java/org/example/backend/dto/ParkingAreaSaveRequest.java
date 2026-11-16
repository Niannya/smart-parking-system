package org.example.backend.dto;

/**
 * 新增 / 修改停车区域的请求体。
 *
 * <p>修改时 lotId 与 areaCode 会被忽略：区域编号已经写进唯一约束和历史数据，
 * 允许改会让「同停车场内编号唯一」这条规则变得无法追溯，所以只允许改名称和说明。</p>
 */
public class ParkingAreaSaveRequest {

    private Long lotId;
    private String areaCode;
    private String areaName;
    private String description;

    public Long getLotId() {
        return lotId;
    }

    public void setLotId(Long lotId) {
        this.lotId = lotId;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
