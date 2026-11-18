package org.example.backend.dto.entry;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 入场结果（成员 2 scaffold，字段同 01 契约第 5 节入场返回）。
 */
@Data
@AllArgsConstructor
public class EntryResultDto {

    private Long id;
    private String recordNo;
    private Long lotId;
    private String licensePlate;
    private Long spaceId;
    private String areaCode;
    private String spaceCode;
    private Long feeRuleId;
    private OffsetDateTime entryTime;
    private String status;
}
