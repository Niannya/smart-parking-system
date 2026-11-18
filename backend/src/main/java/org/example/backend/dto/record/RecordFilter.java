package org.example.backend.dto.record;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 管理员记录查询过滤项（01 契约第 6 节）。
 * 过滤项均可省；from/to 应同时提供；空字符串过滤值报 400。
 * 字段名 entryFrom/entryTo 对应接口的 from/to（避开 OGNL 对 from 的解析歧义）。
 */
@Data
public class RecordFilter {

    private Long lotId;
    private String licensePlate;
    private String status;
    private Long spaceId;
    private OffsetDateTime entryFrom;
    private OffsetDateTime entryTo;
}
