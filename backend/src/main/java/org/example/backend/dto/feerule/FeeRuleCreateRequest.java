package org.example.backend.dto.feerule;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新建收费规则请求（01 契约第 4 节）。创建即激活；
 * 历史规则不可改删，无 PUT/DELETE。
 */
@Data
public class FeeRuleCreateRequest {

    @NotNull(message = "停车场不能为空")
    private Long lotId;

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称最长 100 字符")
    private String ruleName;

    @NotNull(message = "免费时长不能为空")
    @Min(value = 0, message = "免费时长不能为负")
    private Integer freeMinutes;

    @NotNull(message = "计费单位不能为空")
    @Min(value = 1, message = "计费单位分钟必须为正数")
    private Integer billingUnitMinutes;

    @NotNull(message = "单位价格不能为空")
    @DecimalMin(value = "0.00", message = "单位价格不能为负")
    @Digits(integer = 8, fraction = 2, message = "单位价格最多 8 位整数、2 位小数")
    private BigDecimal unitPrice;

    @NotNull(message = "24 小时封顶费不能为空")
    @DecimalMin(value = "0.00", message = "24 小时封顶费不能为负")
    @Digits(integer = 8, fraction = 2, message = "24 小时封顶费最多 8 位整数、2 位小数")
    private BigDecimal maxFeePer24h;
}
