package org.example.backend.dto.space;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新车位请求（成员 2 scaffold）。
 */
@Data
public class ParkingSpaceCreateRequest {

    @NotNull(message = "区域不能为空")
    @Positive(message = "areaId 必须为正整数")
    private Long areaId;

    @NotBlank(message = "车位编号不能为空")
    @Size(max = 32, message = "车位编号最长 32 字符")
    private String spaceCode;

    @Size(max = 500, message = "说明最长 500 字符")
    private String description;
}
