package org.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.OffsetDateTime;

/**
 * 车主车辆，对应表 vehicle。
 *
 * <p>接口契约规定 Vehicle 不向客户端暴露归属账号，因此 {@code ownerUserId} 用
 * {@link JsonIgnore} 隐藏；它只用于写入（且值只能来自当前会话）。归属过滤全部在
 * SQL 的 {@code WHERE owner_user_id = 当前会话用户} 完成。</p>
 */
public class Vehicle {

    private Long id;
    private String licensePlate;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /** 仅用于 INSERT，不参与序列化。 */
    @JsonIgnore
    private Long ownerUserId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
}
