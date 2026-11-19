package org.example.backend.dto;

/**
 * 车主绑定车辆请求体：{@code {"licensePlate":"京A12345"}}。
 *
 * <p>不允许出现 {@code ownerUserId} 等字段，归属只能来自当前会话。</p>
 */
public class OwnerVehicleCreateRequest extends RejectUnknownJsonRequest {

    private String licensePlate;

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }
}
