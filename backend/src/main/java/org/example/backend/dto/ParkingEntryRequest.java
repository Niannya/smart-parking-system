package org.example.backend.dto;

/**
 * 车辆入场请求体：{@code {"lotId":1,"licensePlate":"京A12345"}}。
 *
 * <p>管理员 ID、规则 ID、车位、入场时间、状态全部由服务端决定；请求里出现这些字段
 * 会被 {@link RejectUnknownJsonRequest} 收集并拒绝（4000）。</p>
 */
public class ParkingEntryRequest extends RejectUnknownJsonRequest {

    private Long lotId;
    private String licensePlate;

    public Long getLotId() {
        return lotId;
    }

    public void setLotId(Long lotId) {
        this.lotId = lotId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }
}
