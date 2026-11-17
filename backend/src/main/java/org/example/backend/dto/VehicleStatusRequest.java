package org.example.backend.dto;

/**
 * 车辆状态请求体：只接受 {@code ACTIVE} 或 {@code DISABLED}，不提供物理删除。
 */
public class VehicleStatusRequest extends RejectUnknownJsonRequest {

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
