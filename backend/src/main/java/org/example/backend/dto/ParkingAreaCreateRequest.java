package org.example.backend.dto;

/**
 * 新增停车区域请求体：{@code {"lotId":1,"areaCode":"C","areaName":"C区","description":null}}。
 */
public class ParkingAreaCreateRequest extends RejectUnknownJsonRequest {

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
