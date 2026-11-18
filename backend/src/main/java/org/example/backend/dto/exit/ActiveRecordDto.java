package org.example.backend.dto.exit;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 在场记录查询结果（01 契约第 5 节 ActiveRecord）。
 */
@Data
public class ActiveRecordDto {

    private Long id;
    private String recordNo;
    private Long lotId;
    private String licensePlate;
    private Long spaceId;
    private String areaCode;
    private String spaceCode;
    private OffsetDateTime entryTime;
    private String status;
}
