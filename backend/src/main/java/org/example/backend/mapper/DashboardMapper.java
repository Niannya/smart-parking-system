package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Mapper
public interface DashboardMapper {

    record Counts(long totalSpaces, long freeSpaces, long occupiedSpaces, long disabledSpaces,
                  long entryCount, long exitCount, BigDecimal totalRevenue) {
    }

    record RevenueCounts(long paymentCount, BigDecimal totalRevenue) {
    }

    /** 指定停车场不存在时返回 null；存在但无数据时各聚合值返回零。 */
    Counts selectSummary(@Param("lotId") Long lotId,
                         @Param("fromTime") OffsetDateTime dayStart,
                         @Param("toTime") OffsetDateTime dayEnd);

    /** 按支付时间的半开区间聚合；指定停车场不存在时返回 null。 */
    RevenueCounts selectRevenue(@Param("lotId") Long lotId,
                                @Param("fromTime") OffsetDateTime fromTime,
                                @Param("toTime") OffsetDateTime toTime);
}
