package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCodes;
import org.example.backend.dto.entry.EntryResultDto;
import org.example.backend.dto.entry.ParkingEntryRequest;
import org.example.backend.dto.exit.AbnormalExitRequest;
import org.example.backend.dto.exit.ExitRequest;
import org.example.backend.dto.exit.ExitResultDto;
import org.example.backend.dto.record.RecordDto;
import org.example.backend.dto.record.RecordFilter;
import org.example.backend.dto.record.PaymentInfoDto;
import org.example.backend.dto.vehicle.VehicleBindRequest;
import org.example.backend.support.PostgresIntegrationTest;
import org.example.backend.support.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/**
 * 停车记录查询数据库集成测试（成员 3，M5 / T20-T22）。
 * 覆盖管理员组合过滤与排序、车主隔离、越权 403、停用绑定历史可见、
 * 临时车历史不因后来绑定开放、PARKING/COMPLETED/ABNORMAL 派生字段。
 * 本机无 PostgreSQL 时按假设跳过；复验命令见 PR 交付说明。
 */
class ParkingRecordQueryServiceTest extends PostgresIntegrationTest {

    @Autowired
    private ParkingRecordQueryService recordQueryService;

    @Autowired
    private ParkingEntryService entryService;

    @Autowired
    private ParkingExitService exitService;

    @Autowired
    private OwnerVehicleService ownerVehicleService;

    /** 固定时钟：PARKING 记录的时长/预估金额按可控时刻计算 */
    @MockBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        TestSecurity.setAdmin(adminId);
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.ofHours(8));
        lenient().when(clock.instant()).thenAnswer(invocation -> Instant.now());
    }

    private void fixClockAt(Instant instant) {
        lenient().when(clock.instant()).thenReturn(instant);
    }

    private EntryResultDto enter(String plate, Instant entryInstant) {
        fixClockAt(entryInstant);
        ParkingEntryRequest request = new ParkingEntryRequest();
        request.setLotId(lot1Id);
        request.setLicensePlate(plate);
        return entryService.entry(request);
    }

    private EntryResultDto enterLot2(String plate, Instant entryInstant) {
        fixClockAt(entryInstant);
        ParkingEntryRequest request = new ParkingEntryRequest();
        request.setLotId(lot2Id);
        request.setLicensePlate(plate);
        return entryService.entry(request);
    }

    private ExitResultDto exitCash(Long recordId, BigDecimal amountPaid) {
        ExitRequest request = new ExitRequest();
        request.setPaymentMethod("CASH");
        request.setAmountPaid(amountPaid);
        return exitService.exit(recordId, request);
    }

    /** 以当前会话车主身份绑定车牌 */
    private void bindAsOwner(Long ownerUserId, String plate) {
        TestSecurity.setOwner(ownerUserId);
        VehicleBindRequest request = new VehicleBindRequest();
        request.setLicensePlate(plate);
        ownerVehicleService.bind(request);
        TestSecurity.setAdmin(adminId);
    }

    private RecordFilter filter() {
        return new RecordFilter();
    }

    @Test
    @DisplayName("T20：管理员组合过滤与排序（entryTime DESC, id DESC）")
    void adminFilterAndSort() {
        // 先全部入场（车位按序分配、互不相同），再离场 A：
        // 若先离场 A 再入场 B，B 会复用 A 释放的车位，按车位过滤就会命中 2 条
        EntryResultDto a = enter(randomPlate(), T0.toInstant());
        EntryResultDto b = enter(randomPlate(), T0.plusMinutes(120).toInstant());
        EntryResultDto c = enterLot2(randomPlate(), T0.plusMinutes(240).toInstant());
        fixClockAt(T0.plusMinutes(61).toInstant());
        exitCash(a.getId(), new BigDecimal("5.00"));
        // 查询时刻推到所有入场之后：PARKING 记录按查询时刻计费，不能早于各自入场时间
        fixClockAt(T0.plusMinutes(300).toInstant());

        // 无过滤：3 条，最新在前
        List<RecordDto> all = recordQueryService.listForAdmin(filter());
        assertEquals(3, all.size());
        assertEquals(c.getId(), all.get(0).getId());
        assertEquals(b.getId(), all.get(1).getId());
        assertEquals(a.getId(), all.get(2).getId());

        // 按场过滤
        RecordFilter byLot = filter();
        byLot.setLotId(lot1Id);
        List<RecordDto> lot1Records = recordQueryService.listForAdmin(byLot);
        assertEquals(2, lot1Records.size());
        assertEquals(b.getId(), lot1Records.get(0).getId());
        assertEquals(a.getId(), lot1Records.get(1).getId());

        // 按车牌过滤（小写也命中：服务端规范化）
        RecordFilter byPlate = filter();
        byPlate.setLicensePlate(a.getLicensePlate().toLowerCase());
        List<RecordDto> plateRecords = recordQueryService.listForAdmin(byPlate);
        assertEquals(1, plateRecords.size());
        assertEquals(a.getId(), plateRecords.get(0).getId());

        // 按状态过滤
        RecordFilter byStatus = filter();
        byStatus.setStatus("PARKING");
        List<RecordDto> parking = recordQueryService.listForAdmin(byStatus);
        assertEquals(2, parking.size());
        assertEquals(c.getId(), parking.get(0).getId());
        assertEquals(b.getId(), parking.get(1).getId());

        // 按车位过滤
        RecordFilter bySpace = filter();
        bySpace.setSpaceId(b.getSpaceId());
        assertEquals(1, recordQueryService.listForAdmin(bySpace).size());

        // 时间范围：from 含等号、to 不含等号（>= from, < to）
        RecordFilter range = filter();
        range.setEntryFrom(T0.toInstant().atOffset(ZoneOffset.ofHours(8)));
        range.setEntryTo(T0.plusMinutes(1).toInstant().atOffset(ZoneOffset.ofHours(8)));
        List<RecordDto> rangeRecords = recordQueryService.listForAdmin(range);
        assertEquals(1, rangeRecords.size());
        assertEquals(a.getId(), rangeRecords.get(0).getId(), "entry_time = from 应包含（>=）");

        RecordFilter range2 = filter();
        range2.setEntryFrom(T0.plusMinutes(60).toInstant().atOffset(ZoneOffset.ofHours(8)));
        range2.setEntryTo(T0.plusMinutes(121).toInstant().atOffset(ZoneOffset.ofHours(8)));
        List<RecordDto> range2Records = recordQueryService.listForAdmin(range2);
        assertEquals(1, range2Records.size());
        assertEquals(b.getId(), range2Records.get(0).getId());

        // 组合：场1 + PARKING
        RecordFilter combined = filter();
        combined.setLotId(lot1Id);
        combined.setStatus("PARKING");
        List<RecordDto> combinedRecords = recordQueryService.listForAdmin(combined);
        assertEquals(1, combinedRecords.size());
        assertEquals(b.getId(), combinedRecords.get(0).getId());
    }

    @Test
    @DisplayName("T20：过滤参数校验（from/to 成对、空串 400、非法状态 400、范围倒置 4701）")
    void adminFilterValidation() {
        RecordFilter fromOnly = filter();
        fromOnly.setEntryFrom(T0.toInstant().atOffset(ZoneOffset.ofHours(8)));
        assertEquals(ErrorCodes.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> recordQueryService.listForAdmin(fromOnly)).getCode());

        RecordFilter toOnly = filter();
        toOnly.setEntryTo(T0.toInstant().atOffset(ZoneOffset.ofHours(8)));
        assertEquals(ErrorCodes.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> recordQueryService.listForAdmin(toOnly)).getCode());

        RecordFilter reversed = filter();
        reversed.setEntryFrom(T0.plusMinutes(10).toInstant().atOffset(ZoneOffset.ofHours(8)));
        reversed.setEntryTo(T0.toInstant().atOffset(ZoneOffset.ofHours(8)));
        assertEquals(ErrorCodes.DATE_RANGE_INVALID, assertThrows(BusinessException.class,
                () -> recordQueryService.listForAdmin(reversed)).getCode());

        RecordFilter emptyPlate = filter();
        emptyPlate.setLicensePlate("   ");
        assertEquals(ErrorCodes.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> recordQueryService.listForAdmin(emptyPlate)).getCode());

        RecordFilter emptyStatus = filter();
        emptyStatus.setStatus("");
        assertEquals(ErrorCodes.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> recordQueryService.listForAdmin(emptyStatus)).getCode());

        RecordFilter badStatus = filter();
        badStatus.setStatus("FINISHED");
        assertEquals(ErrorCodes.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> recordQueryService.listForAdmin(badStatus)).getCode());

        // 小写状态合法（归一化后过滤）
        RecordFilter lowerStatus = filter();
        lowerStatus.setStatus("parking");
        assertTrue(recordQueryService.listForAdmin(lowerStatus).isEmpty());
    }

    @Test
    @DisplayName("T21：管理员详情存在即返回；不存在 4601")
    void adminDetail() {
        EntryResultDto entry = enter(randomPlate(), T0.toInstant());
        RecordDto dto = recordQueryService.getForAdmin(entry.getId());
        assertEquals(entry.getRecordNo(), dto.getRecordNo());
        assertEquals("中心停车场", dto.getLotName());
        assertEquals(entry.getLicensePlate(), dto.getLicensePlate());
        assertEquals(entry.getAreaCode(), dto.getAreaCode());
        assertEquals(entry.getSpaceCode(), dto.getSpaceCode());

        assertEquals(ErrorCodes.RECORD_NOT_FOUND, assertThrows(BusinessException.class,
                () -> recordQueryService.getForAdmin(999999999L)).getCode());
    }

    @Test
    @DisplayName("T22：两个车主记录隔离（JOIN vehicle.owner_user_id，不凭车牌快照）")
    void ownerIsolationBetweenTwoOwners() {
        String plate1 = randomPlate();
        bindAsOwner(owner1Id, plate1);
        EntryResultDto owned = enter(plate1, T0.toInstant());

        // owner1 未绑定的临时车记录
        enter(randomPlate(), T0.plusMinutes(60).toInstant());

        // owner2 的名下车辆记录
        String plate2 = randomPlate();
        bindAsOwner(owner2Id, plate2);
        EntryResultDto other = enter(plate2, T0.plusMinutes(120).toInstant());

        TestSecurity.setOwner(owner1Id);
        List<RecordDto> owner1Records = recordQueryService.listForOwner(null);
        assertEquals(1, owner1Records.size());
        assertEquals(owned.getId(), owner1Records.get(0).getId());

        TestSecurity.setOwner(owner2Id);
        List<RecordDto> owner2Records = recordQueryService.listForOwner(null);
        assertEquals(1, owner2Records.size());
        assertEquals(other.getId(), owner2Records.get(0).getId());

        // 车主按状态过滤
        TestSecurity.setOwner(owner1Id);
        RecordDto byStatus = recordQueryService.listForOwner("COMPLETED").stream().findFirst().orElse(null);
        assertNull(byStatus);
        assertEquals(1, recordQueryService.listForOwner("PARKING").size());
    }

    @Test
    @DisplayName("T22：车主详情越权 4602；不存在 4601")
    void ownerDetailForbidden() {
        String plate = randomPlate();
        bindAsOwner(owner1Id, plate);
        EntryResultDto entry = enter(plate, T0.toInstant());

        // 本人可看
        TestSecurity.setOwner(owner1Id);
        assertEquals(entry.getId(), recordQueryService.getForOwner(entry.getId()).getId());

        // 他人越权：403/4602
        TestSecurity.setOwner(owner2Id);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> recordQueryService.getForOwner(entry.getId()));
        assertEquals(ErrorCodes.RECORD_NOT_OWNED, ex.getCode());

        // 不存在的记录：404/4601（而不是 403）
        assertEquals(ErrorCodes.RECORD_NOT_FOUND, assertThrows(BusinessException.class,
                () -> recordQueryService.getForOwner(999999999L)).getCode());
    }

    @Test
    @DisplayName("T22：停用绑定后历史记录仍对车主可见")
    void disabledVehicleHistoryStillVisible() {
        String plate = randomPlate();
        bindAsOwner(owner1Id, plate);
        EntryResultDto entry = enter(plate, T0.toInstant());

        // 车主停用该车辆绑定
        TestSecurity.setOwner(owner1Id);
        Long vehicleId = recordQueryService.getForOwner(entry.getId()).getVehicleId();
        ownerVehicleService.updateStatus(vehicleId, "DISABLED");

        // 历史记录仍可见（JOIN 不过滤 vehicle.status）
        List<RecordDto> records = recordQueryService.listForOwner(null);
        assertEquals(1, records.size());
        assertEquals(entry.getId(), records.get(0).getId());
        assertNotNull(recordQueryService.getForOwner(entry.getId()));
    }

    @Test
    @DisplayName("T22：临时车历史不因后来绑定而对车主开放")
    void temporaryHistoryNotOpenedByLaterBinding() {
        String plate = randomPlate();
        // 先以临时车入场（当时未绑定，vehicle_id 为空）
        EntryResultDto entry = enter(plate, T0.toInstant());
        assertNull(recordQueryService.getForAdmin(entry.getId()).getVehicleId());

        // 之后车主才绑定该车牌
        bindAsOwner(owner1Id, plate);

        TestSecurity.setOwner(owner1Id);
        assertTrue(recordQueryService.listForOwner(null).isEmpty(),
                "绑定前的临时车记录不因后来绑定而开放");
        // 记录存在但不属于本人：403/4602（与“看到别人记录”同一语义，不泄露存在性差异）
        assertEquals(ErrorCodes.RECORD_NOT_OWNED, assertThrows(BusinessException.class,
                () -> recordQueryService.getForOwner(entry.getId())).getCode());

        // 管理员仍可见
        assertEquals(entry.getId(), recordQueryService.getForAdmin(entry.getId()).getId());
    }

    @Test
    @DisplayName("T21：PARKING 记录按查询时刻派生：UNPAID + estimatedAmountDue + payment=null")
    void parkingRecordDerivedFields() {
        EntryResultDto entry = enter(randomPlate(), T0.toInstant());

        // 查询时刻 61 分钟：31 计费分钟 → 1 个单位 5 元
        fixClockAt(T0.plusMinutes(61).toInstant());
        RecordDto dto = recordQueryService.getForAdmin(entry.getId());
        assertEquals("PARKING", dto.getStatus());
        assertEquals(61L, dto.getDurationMinutes());
        assertEquals(new BigDecimal("5.00"), dto.getEstimatedAmountDue());
        assertEquals("UNPAID", dto.getPaymentStatus());
        assertNull(dto.getPayment());
        assertNull(dto.getExitTime());

        // 查询时刻 30 分钟：免费时长内，预估 0 元
        fixClockAt(T0.plusMinutes(30).toInstant());
        RecordDto free = recordQueryService.getForAdmin(entry.getId());
        assertEquals(30L, free.getDurationMinutes());
        assertEquals(new BigDecimal("0.00"), free.getEstimatedAmountDue());
    }

    @Test
    @DisplayName("T21：COMPLETED 正常收款 PAID / 免费放行 WAIVED / ABNORMAL 无支付")
    void completedRecordPaymentStatuses() {
        // 正常收款
        EntryResultDto cash = enter(randomPlate(), T0.toInstant());
        fixClockAt(T0.plusMinutes(61).toInstant());
        ExitResultDto cashResult = exitCash(cash.getId(), new BigDecimal("5.00"));
        RecordDto cashDto = recordQueryService.getForAdmin(cash.getId());
        assertEquals("COMPLETED", cashDto.getStatus());
        assertEquals("PAID", cashDto.getPaymentStatus());
        assertNull(cashDto.getEstimatedAmountDue(), "非在场记录不给预估金额");
        assertEquals(cashResult.getPaymentNo(), cashDto.getPayment().paymentNo());
        assertEquals(new BigDecimal("5.00"), cashDto.getPayment().amountPaid());
        assertEquals("CASH", cashDto.getPayment().paymentMethod());
        assertEquals(61L, cashDto.getDurationMinutes());

        // 免费放行
        EntryResultDto free = enter(randomPlate(), T0.plusMinutes(120).toInstant());
        fixClockAt(T0.plusMinutes(181).toInstant());
        ExitRequest freeRequest = new ExitRequest();
        freeRequest.setPaymentMethod("FREE_RELEASE");
        freeRequest.setAmountPaid(BigDecimal.ZERO.setScale(2));
        freeRequest.setRemark("人工核实后减免");
        ExitResultDto freeResult = exitService.exit(free.getId(), freeRequest);
        RecordDto freeDto = recordQueryService.getForAdmin(free.getId());
        assertEquals("COMPLETED", freeDto.getStatus());
        assertEquals("WAIVED", freeDto.getPaymentStatus());
        assertEquals(freeResult.getPaymentNo(), freeDto.getPayment().paymentNo());
        assertEquals(new BigDecimal("0.00"), freeDto.getPayment().amountPaid());
        assertEquals(new BigDecimal("5.00"), freeDto.getPayment().amountDue());

        // 异常离场
        EntryResultDto abnormal = enter(randomPlate(), T0.plusMinutes(240).toInstant());
        fixClockAt(T0.plusMinutes(301).toInstant());
        exitService.abnormal(abnormal.getId(), abnormalRequest("道闸故障人工放行"));
        RecordDto abnormalDto = recordQueryService.getForAdmin(abnormal.getId());
        assertEquals("ABNORMAL", abnormalDto.getStatus());
        assertEquals("ABNORMAL", abnormalDto.getPaymentStatus());
        assertNull(abnormalDto.getPayment());
        assertEquals("道闸故障人工放行", abnormalDto.getAbnormalReason());
        assertNull(abnormalDto.getEstimatedAmountDue());
    }

    private AbnormalExitRequest abnormalRequest(String reason) {
        AbnormalExitRequest request = new AbnormalExitRequest();
        request.setReason(reason);
        return request;
    }
}
