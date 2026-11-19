package org.example.backend.dto;

/**
 * 新增停车位请求体：{@code {"areaId":2,"spaceCode":"011","description":null}}。
 *
 * <p>归属停车场不在请求里传，由 {@code parking_area -> parking_lot} 反查，避免请求体伪造 lotId。</p>
 */
public class ParkingSpaceCreateRequest extends RejectUnknownJsonRequest {

    private Long areaId;
    private String spaceCode;
    private String description;

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

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
