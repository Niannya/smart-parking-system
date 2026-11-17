package org.example.backend.integration;

import org.example.backend.common.BusinessException;
import org.example.backend.dto.ParkingEntryRequest;
import org.example.backend.dto.ParkingEntryResult;
import org.example.backend.service.ParkingEntryDependency;
import org.example.backend.service.ParkingEntryService;
import org.example.backend.support.CurrentUserProvider;
import org.example.backend.support.CurrentUserView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 入场事务的真实 PostgreSQL 测试。
 *
 * <p>默认 {@code mvn test} 会跳过本类；需要先确认 {@code smart_parking_test} 存在种子数据，
 * 再执行：{@code .\mvnw.cmd -o test -Dparking.it=true -Dtest=ParkingEntryPostgresIT}。</p>
 *
 * <p>测试用的 {@link ParkingEntryDependency} 只在本类内用 JdbcTemplate 模拟成员 1/3 的
 * 锁场与规则查询，生产代码不会复制这段逻辑；生产替换方式见操作说明文档。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/smart_parking_test",
        "spring.datasource.username=postgres",
        "spring.datasource.password=123456",
        "spring.datasource.driver-class-name=org.postgresql.Driver"
})
@EnabledIfSystemProperty(named = "parking.it", matches = "true")
class ParkingEntryPostgresIT {

    @Autowired
    private ParkingEntryService parkingEntryService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUp() {
        resetDatabase();
        when(currentUserProvider.currentUser())
                .thenReturn(new CurrentUserView(1L, CurrentUserView.ROLE_ADMIN));
    }

    @AfterEach
    void tearDown() {
        resetDatabase();
    }

    @Test
    void allocatesFirstFreeSpaceInFixedAreaSpaceOrder() {
        Long expectedSpaceId = firstFreeSpace(1L);
        int freeBefore = countFreeSpaces(1L);

        ParkingEntryResult result = parkingEntryService.enter(request("京A99999", 1L));

        assertEquals(1L, result.getLotId());
        assertEquals(expectedSpaceId, result.getSpaceId());
        assertEquals("PARKING", result.getStatus());
        assertNotNull(result.getRecordNo());
        assertEquals(32, result.getRecordNo().length());
        assertEquals(freeBefore - 1, countFreeSpaces(1L));
        assertEquals("OCCUPIED", spaceStatus(expectedSpaceId));
        assertEquals(1, countRecords("京A99999"));
        assertNull(jdbcTemplate.queryForObject(
                "SELECT vehicle_id FROM parking_record WHERE record_no = ?", Long.class, result.getRecordNo()));
    }

    @Test
    void doesNotCrossIntoAnotherLot() {
        Long lot1Space = firstFreeSpace(1L);
        Long lot2Space = firstFreeSpace(2L);
        assertTrue(lot1Space != null && lot2Space != null);

        ParkingEntryResult result = parkingEntryService.enter(request("京A88888", 2L));

        assertEquals(2L, result.getLotId());
        assertEquals(lot2Space, result.getSpaceId());
        assertEquals("OCCUPIED", spaceStatus(lot2Space));
        assertEquals("FREE", spaceStatus(lot1Space));
    }

    @Test
    void rejectsWhenLotHasNoActiveFeeRuleAndLeavesSpaceFree() {
        int freeBefore = countFreeSpaces(1L);
        jdbcTemplate.update("UPDATE fee_rule SET is_active = FALSE WHERE parking_lot_id = 1");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> parkingEntryService.enter(request("京A77777", 1L)));

        assertEquals(4303, exception.getCode());
        assertEquals(freeBefore, countFreeSpaces(1L));
        assertEquals(0, countRecords("京A77777"));
    }

    @Test
    void rejectsDisabledLotAndLeavesSpaceFree() {
        int freeBefore = countFreeSpaces(1L);
        jdbcTemplate.update("UPDATE parking_lot SET status = 'DISABLED' WHERE id = 1");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> parkingEntryService.enter(request("京A66666", 1L)));

        assertEquals(2005, exception.getCode());
        assertEquals(freeBefore, countFreeSpaces(1L));
    }

    @Test
    void rejectsSamePlateAlreadyParking() {
        parkingEntryService.enter(request("京A55555", 1L));
        int occupiedBefore = countOccupiedSpaces(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> parkingEntryService.enter(request("京A55555", 2L)));

        assertEquals(4301, exception.getCode());
        assertEquals(1, countRecords("京A55555"));
        assertEquals(occupiedBefore, countOccupiedSpaces(1L) + countOccupiedSpaces(2L));
    }

    @Test
    void insertFailureRollsBackOccupiedSpace() {
        int freeBefore = countFreeSpaces(1L);
        // entry_admin_id 外键指向不存在的用户，插入失败，验证车位占用一起回滚
        when(currentUserProvider.currentUser())
                .thenReturn(new CurrentUserView(999L, CurrentUserView.ROLE_ADMIN));

        assertThrows(DataIntegrityViolationException.class,
                () -> parkingEntryService.enter(request("京A44444", 1L)));

        assertEquals(freeBefore, countFreeSpaces(1L));
        assertEquals(0, countRecords("京A44444"));
    }

    @Test
    void disabledVehicleIsStoredAsTemporaryCar() {
        jdbcTemplate.update("UPDATE vehicle SET status = 'DISABLED' WHERE license_plate = '京A12345'");

        ParkingEntryResult result = parkingEntryService.enter(request("京A12345", 1L));

        Long vehicleId = jdbcTemplate.queryForObject(
                "SELECT vehicle_id FROM parking_record WHERE record_no = ?", Long.class, result.getRecordNo());
        assertNull(vehicleId);
    }

    @Test
    void lastFreeSpaceAllowsOnlyOneConcurrentEntry() throws Exception {
        Long lastSpaceId = firstFreeSpace(1L);
        assertNotNull(lastSpaceId);
        jdbcTemplate.update("UPDATE parking_space SET status = 'DISABLED'");
        jdbcTemplate.update("UPDATE parking_space SET status = 'FREE' WHERE id = ?", lastSpaceId);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Object> first = pool.submit(() -> enterAfterSignal(start, "京A11111"));
            Future<Object> second = pool.submit(() -> enterAfterSignal(start, "京A22222"));
            start.countDown();

            Object one = first.get(10, TimeUnit.SECONDS);
            Object two = second.get(10, TimeUnit.SECONDS);
            int successes = (one instanceof ParkingEntryResult ? 1 : 0) + (two instanceof ParkingEntryResult ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(1, countRecords("京A11111") + countRecords("京A22222"));
            assertEquals("OCCUPIED", spaceStatus(lastSpaceId));
        } finally {
            pool.shutdownNow();
        }
    }

    private Object enterAfterSignal(CountDownLatch start, String plate) throws InterruptedException {
        start.await();
        ParkingEntryRequest request = request(plate, 1L);
        try {
            return parkingEntryService.enter(request);
        } catch (BusinessException exception) {
            return exception.getCode();
        } catch (DataIntegrityViolationException exception) {
            return 4304;
        }
    }

    private ParkingEntryRequest request(String plate, Long lotId) {
        ParkingEntryRequest request = new ParkingEntryRequest();
        request.setLotId(lotId);
        request.setLicensePlate(plate);
        return request;
    }

    private Long firstFreeSpace(Long lotId) {
        List<Long> ids = jdbcTemplate.query(
                "SELECT s.id FROM parking_space s JOIN parking_area a ON a.id = s.area_id " +
                        "WHERE a.parking_lot_id = ? AND a.status = 'ACTIVE' AND s.status = 'FREE' " +
                        "ORDER BY a.area_code, s.space_code, s.id LIMIT 1",
                (rs, rowNum) -> rs.getLong(1), lotId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private int countFreeSpaces(Long lotId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM parking_space s JOIN parking_area a ON a.id = s.area_id " +
                        "WHERE a.parking_lot_id = ? AND a.status = 'ACTIVE' AND s.status = 'FREE'",
                Integer.class, lotId);
    }

    private int countOccupiedSpaces(Long lotId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM parking_space s JOIN parking_area a ON a.id = s.area_id " +
                        "WHERE a.parking_lot_id = ? AND s.status = 'OCCUPIED'",
                Integer.class, lotId);
    }

    private int countRecords(String plate) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM parking_record WHERE license_plate = ? AND status = 'PARKING'",
                Integer.class, plate);
    }

    private String spaceStatus(Long spaceId) {
        return jdbcTemplate.queryForObject("SELECT status FROM parking_space WHERE id = ?", String.class, spaceId);
    }

    private void resetDatabase() {
        jdbcTemplate.update("DELETE FROM parking_record");
        jdbcTemplate.update("UPDATE parking_space SET status = 'FREE'");
        jdbcTemplate.update("UPDATE parking_area SET status = 'ACTIVE'");
        jdbcTemplate.update("UPDATE parking_lot SET status = 'ACTIVE'");
        jdbcTemplate.update("UPDATE vehicle SET status = 'ACTIVE'");
        jdbcTemplate.update("UPDATE fee_rule SET is_active = TRUE");
    }

    @TestConfiguration
    static class TestDependencyConfiguration {

        @Bean
        @Primary
        ParkingEntryDependency parkingEntryDependency(JdbcTemplate jdbcTemplate) {
            return new ParkingEntryDependency() {
                @Override
                public void lockActiveLot(Long lotId) {
                    List<String> statuses = jdbcTemplate.query(
                            "SELECT status FROM parking_lot WHERE id = ? FOR UPDATE",
                            (rs, rowNum) -> rs.getString(1), lotId);
                    if (statuses.isEmpty()) {
                        throw new BusinessException(2001, "停车场不存在");
                    }
                    if (!"ACTIVE".equals(statuses.get(0))) {
                        throw new BusinessException(2005, "该停车场已停用，不能办理入场");
                    }
                }

                @Override
                public Long findActiveFeeRuleId(Long lotId) {
                    List<Long> ids = jdbcTemplate.query(
                            "SELECT id FROM fee_rule WHERE parking_lot_id = ? AND is_active = TRUE " +
                                    "ORDER BY effective_from DESC, id DESC LIMIT 1",
                            (rs, rowNum) -> rs.getLong(1), lotId);
                    return ids.isEmpty() ? null : ids.get(0);
                }
            };
        }
    }
}
