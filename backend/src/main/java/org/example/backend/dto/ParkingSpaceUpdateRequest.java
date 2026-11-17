package org.example.backend.dto;

/**
 * 修改停车位请求体：{@code {"spaceCode":"011","description":"靠入口"}}。
 *
 * <p>只允许 FREE 车位修改；改编号时若已有停车历史会返回 409/4104。</p>
 */
public class ParkingSpaceUpdateRequest extends RejectUnknownJsonRequest {

    private String spaceCode;
    private String description;

    public String getSpaceCode() {
        return spaceCode;
    }

    public void setSpaceCode(String spaceCode) {
        this.spaceCode = spaceCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
