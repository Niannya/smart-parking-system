package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCode;
import org.example.backend.dto.DashboardRevenueDto;
import org.example.backend.dto.DashboardSummaryDto;
import org.example.backend.mapper.DashboardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Service
public class DashboardService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Pattern LOT_ID_FORMAT = Pattern.compile("[0-9]+");
    private static final Pattern DATE_FORMAT = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO_RATE = BigDecimal.ZERO.setScale(2);

    private final DashboardMapper dashboard;
    private final Clock clock;

    @Autowired
    public DashboardService(DashboardMapper dashboard) {
        this(dashboard, Clock.system(BUSINESS_ZONE));
    }

    DashboardService(DashboardMapper dashboard, Clock clock) {
        this.dashboard = dashboard;
        this.clock = clock.withZone(BUSINESS_ZONE);
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto summary(String rawLotId, String rawDate) {
        Long lotId = parseLotId(rawLotId);
        LocalDate date = parseDate(rawDate);
        OffsetDateTime dayStart = date.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        OffsetDateTime dayEnd = date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();

        DashboardMapper.Counts counts = dashboard.selectSummary(lotId, dayStart, dayEnd);
        if (counts == null) {
            throw new BusinessException(ErrorCode.LOT_NOT_FOUND, "停车场不存在");
        }

        long enabledSpaces = counts.freeSpaces() + counts.occupiedSpaces();
        BigDecimal occupancyRate = enabledSpaces == 0 ? ZERO_RATE
                : BigDecimal.valueOf(counts.occupiedSpaces())
                        .multiply(ONE_HUNDRED)
                        .divide(BigDecimal.valueOf(enabledSpaces), 2, RoundingMode.HALF_UP);

        return new DashboardSummaryDto(lotId, date, counts.totalSpaces(), counts.freeSpaces(),
                counts.occupiedSpaces(), counts.disabledSpaces(), occupancyRate,
                counts.entryCount(), counts.exitCount(), counts.totalRevenue().setScale(2));
    }

    @Transactional(readOnly = true)
    public DashboardRevenueDto revenue(String rawLotId, String rawFrom, String rawTo) {
        Long lotId = parseLotId(rawLotId);
        OffsetDateTime from = parseRangeEndpoint(rawFrom, "from");
        OffsetDateTime to = parseRangeEndpoint(rawTo, "to");
        if (!from.isBefore(to)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE, "from 必须早于 to");
        }

        DashboardMapper.RevenueCounts counts = dashboard.selectRevenue(
                lotId, toDatabaseBoundary(from), toDatabaseBoundary(to));
        if (counts == null) {
            throw new BusinessException(ErrorCode.LOT_NOT_FOUND, "停车场不存在");
        }
        return new DashboardRevenueDto(lotId, from, to, counts.paymentCount(), counts.totalRevenue().setScale(2));
    }

    private static OffsetDateTime parseRangeEndpoint(String rawValue, String parameter) {
        if (rawValue != null) {
            try {
                OffsetDateTime value = OffsetDateTime.parse(rawValue);
                if (value.getYear() >= 1 && value.getYear() <= 9999) {
                    return value;
                }
                throw new BusinessException(ErrorCode.INVALID_DATE_RANGE, parameter + " 的年份须在 0001–9999 之间");
            } catch (DateTimeParseException ignored) {
                // 缺少时区偏移或时间无效时，统一返回区间参数错误。
            }
        }
        throw new BusinessException(ErrorCode.INVALID_DATE_RANGE, parameter + " 必须是带时区偏移的 ISO 8601 时间");
    }

    private static OffsetDateTime toDatabaseBoundary(OffsetDateTime value) {
        OffsetDateTime utc = value.withOffsetSameInstant(ZoneOffset.UTC);
        // 库内支付时间只到微秒；两侧向上取至微秒，保持 >= from 和 < to 的判断不变。
        int remainder = utc.getNano() % 1_000;
        return remainder == 0 ? utc : utc.plusNanos(1_000 - remainder);
    }

    private static Long parseLotId(String rawLotId) {
        if (rawLotId == null) {
            return null;
        }
        if (LOT_ID_FORMAT.matcher(rawLotId).matches()) {
            try {
                long lotId = Long.parseLong(rawLotId);
                if (lotId > 0) {
                    return lotId;
                }
            } catch (NumberFormatException ignored) {
                // 超过 BIGINT 范围时按非法 ID 处理。
            }
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST, "停车场 ID 必须是正整数");
    }

    private LocalDate parseDate(String rawDate) {
        if (rawDate == null) {
            return LocalDate.now(clock);
        }
        if (DATE_FORMAT.matcher(rawDate).matches()) {
            try {
                LocalDate date = LocalDate.parse(rawDate);
                if (date.getYear() > 0) {
                    return date;
                }
            } catch (DateTimeParseException ignored) {
                // 格式正确的字符串也可能不是有效日期。
            }
        }
        throw new BusinessException(ErrorCode.INVALID_DATE_RANGE, "日期必须是有效的 YYYY-MM-DD");
    }
}
