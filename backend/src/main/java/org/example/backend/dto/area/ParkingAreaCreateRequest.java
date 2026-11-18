package org.example.backend.dto.area;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建区域请求（成员 2 scaffold）。
 */
@Data
public class ParkingAreaCreateRequest {

    @NotNull(message = "停车场不能为空")
    @Positive(message = "lotId 必须为正整数")
    private Long lotId;

    @NotBlank(message = "区域编号不能为空")
    @Size(max = 32, message = "区域编号最长 32 字符")
    private String areaCode;

    @NotBlank(message = "区域名称不能为空")
    @Size(max = 100, message = "区域名称最长 100 字符")
    private String areaName;

    @Size(max = 500, message = "说明最长 500 字符")
    private String description;
}
