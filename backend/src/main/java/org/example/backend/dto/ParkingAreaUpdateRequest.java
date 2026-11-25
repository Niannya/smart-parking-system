package org.example.backend.dto;

/**
 * 修改停车区域请求体：只允许名称与说明。
 *
 * <p>归属（lotId）与编号（areaCode）创建后不可改；请求里出现这些字段会被拒绝（400/4000），
 * 不会「静默忽略」，避免前端误以为修改成功。</p>
 */
public class ParkingAreaUpdateRequest extends RejectUnknownJsonRequest {

    private String areaName;
    private String description;

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
