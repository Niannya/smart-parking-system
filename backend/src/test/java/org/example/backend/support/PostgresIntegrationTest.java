package org.example.backend.support;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * PostgreSQL 集成测试基类（成员 3 测试）。
 *
 * 本机没有 PostgreSQL 时整个类按假设跳过（不会误报通过）；
 * 在组员机器上配置 PARKING_DB_URL/PARKING_DB_USER/PARKING_DB_PASSWORD 后重跑即真实执行。
 * @BeforeAll 重建 schema 并写入演示数据；@AfterEach 清空记录/支付并复位车位，保证用例互不依赖。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class PostgresIntegrationTest {

    /** 固定时刻：2026-11-16T10:00:00+08:00，时间相关用例不等待真实时长 */
    protected static final OffsetDateTime T0 =
            OffsetDateTime.of(2026, 11, 16, 10, 0, 0, 0, ZoneOffset.ofHours(8));

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected static Long adminId;
    protected static Long owner1Id;
    protected static Long owner2Id;
    protected static Long lot1Id;
    protected static Long lot2Id;

    @BeforeAll
    void rebuildDatabase() throws Exception {
        String url = System.getenv().getOrDefault("PARKING_DB_URL", "jdbc:postgresql://localhost:5432/smart_parking_test");
        String user = System.getenv().getOrDefault("PARKING_DB_USER", "postgres");
        String password = System.getenv().getOrDefault("PARKING_DB_PASSWORD", "postgres");
        assumeTrue(dbAvailable(url, user, password),
                "本地无可用 PostgreSQL（" + url + "），跳过数据库集成测试；复验命令见 PR 交付说明");

        jdbcTemplate.execute("DROP SCHEMA public CASCADE");
        jdbcTemplate.execute("CREATE SCHEMA public");
        try (Connection connection = dataSource.getConnection()) {
            ScriptRunner runner = new ScriptRunner(connection);
            runner.setAutoCommit(true);
            runner.setLogWriter(null);
            runner.setErrorLogWriter(null);
            runner.runScript(reader("/database/01_schema.sql"));
            runner.runScript(reader("/database/02_seed_data.sql"));
        }

        adminId = jdbcTemplate.queryForObject("SELECT id FROM user_account WHERE username = 'admin'", Long.class);
        owner1Id = jdbcTemplate.queryForObject("SELECT id FROM user_account WHERE username = 'owner1'", Long.class);
        owner2Id = jdbcTemplate.queryForObject("SELECT id FROM user_account WHERE username = 'owner2'", Long.class);
        lot1Id = jdbcTemplate.queryForObject("SELECT id FROM parking_lot WHERE lot_code = 'LOT-001'", Long.class);
        lot2Id = jdbcTemplate.queryForObject("SELECT id FROM parking_lot WHERE lot_code = 'LOT-002'", Long.class);
        assertNotNull(adminId);
        assertNotNull(owner1Id);
        assertNotNull(lot1Id);
    }

    @BeforeEach
    void resetRecords() {
        // 种子演示记录（京A12345 已完成 / 川B00001 在场）在每个用例前清掉，
        // 保证用例计数确定、与执行顺序无关；@AfterEach 再清一次兜底
        truncateRecords();
        TestSecurity.clear();
    }

    @AfterEach
    void resetRecordsAfter() {
        truncateRecords();
        TestSecurity.clear();
    }

    private void truncateRecords() {
        jdbcTemplate.execute("TRUNCATE parking_record, payment_record RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("UPDATE parking_space SET status = 'FREE', updated_at = now()");
        // 收费规则复位到种子状态：同一 Spring 上下文内多个测试类共享数据库，
        // 规则"创建即激活"会写坏后续用例的预期金额（如 61 分钟从 5.00 变 18.00）
        jdbcTemplate.execute("DELETE FROM fee_rule");
        jdbcTemplate.execute("INSERT INTO fee_rule (parking_lot_id, rule_name, free_minutes, "
                + "billing_unit_minutes, unit_price, max_fee_per24h, is_active, effective_from, created_by) "
                + "SELECT l.id, '日常规则', 30, 60, 5.00, 50.00, true, now(), "
                + "(SELECT id FROM user_account WHERE username = 'admin') FROM parking_lot l");
    }

    @AfterAll
    void clearSecurity() {
        TestSecurity.clear();
    }

    private Reader reader(String resource) {
        return new InputStreamReader(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8);
    }

    private boolean dbAvailable(String url, String user, String password) {
        try (Connection ignored = DriverManager.getConnection(url, user, password)) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /** 生成合规车牌：京A + 5 位大写十六进制（匹配课程约定的小型车校验） */
    protected static String randomPlate() {
        return "京A" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
    }

    /** 生成不重复的停车场编号 */
    protected static String randomLotCode() {
        return "LOT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
}
