package org.example.backend.dto;

/**
 * 车位状态请求体：只接受 {@code FREE} 或 {@code DISABLED}。
 * {@code OCCUPIED} 由入场/离场事务维护，外部传入按参数错误 4000 拒绝。
 */
public class ParkingSpaceStatusRequest extends RejectUnknownJsonRequest {

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
