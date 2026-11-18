package org.example.backend.dto.exit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 异常离场请求：需求中保留的人工处置通道，原因必填。
 */
@Data
public class AbnormalExitRequest {

    @NotBlank(message = "异常离场原因必填")
    @Size(max = 500, message = "异常离场原因最长 500 字符")
    private String reason;
}
