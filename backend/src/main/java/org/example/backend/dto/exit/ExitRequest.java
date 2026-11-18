package org.example.backend.dto.exit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 离场确认请求（01 契约第 5 节）。
 * CASH/ELECTRONIC：amountPaid 必须等于服务端应收额（金额 0 也允许）；
 * FREE_RELEASE：强制 amountPaid=0 且 remark 非空（减免操作，保留原 amountDue 便于核对）。
 * 这里是模拟确认收款，不接真实支付渠道。
 */
@Data
public class ExitRequest {

    @NotBlank(message = "支付方式不能为空")
    @Pattern(regexp = "^(CASH|ELECTRONIC|FREE_RELEASE)$", message = "支付方式仅支持 CASH/ELECTRONIC/FREE_RELEASE")
    private String paymentMethod;

    @DecimalMin(value = "0.00", message = "实收金额不能为负")
    private BigDecimal amountPaid;

    @Size(max = 500, message = "备注最长 500 字符")
    private String remark;
}
