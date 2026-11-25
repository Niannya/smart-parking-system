package org.example.backend.dto;

/**
 * 状态切换请求体，例如 {@code {"status":"DISABLED"}}。
 */
public class ParkingAreaStatusRequest extends RejectUnknownJsonRequest {

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
