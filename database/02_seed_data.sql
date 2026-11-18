-- =============================================================
-- 智能停车场管理系统 演示数据（虚构账号，仅用于本地演示）
-- 两个停车场；每场两个区域、每区域 10 个车位；每场一条生效收费规则；
-- 演示账号：admin / admin123456；owner1 / owner123456；owner2 / owner123456
-- 密码 hash 由 pgcrypto crypt(..., gen_salt('bf')) 生成，与 Spring Security BCrypt 校验一致
-- =============================================================

-- ---------- 账号 ----------
INSERT INTO user_account (username, password_hash, display_name, role, status) VALUES
    ('admin',  crypt('admin123456', gen_salt('bf')), '系统管理员', 'ADMIN', 'ACTIVE'),
    ('owner1', crypt('owner123456', gen_salt('bf')), '车主甲',     'OWNER', 'ACTIVE'),
    ('owner2', crypt('owner123456', gen_salt('bf')), '车主乙',     'OWNER', 'ACTIVE');

-- ---------- 停车场 ----------
INSERT INTO parking_lot (lot_code, lot_name, address, description, status) VALUES
    ('LOT-001', '中心停车场', '示例路1号', '课设演示主停车场', 'ACTIVE'),
    ('LOT-002', '东区停车场', '示例路2号', '课设演示第二停车场', 'ACTIVE');

-- ---------- 区域：每场 A/B 两个区域 ----------
INSERT INTO parking_area (parking_lot_id, area_code, area_name, description, status)
SELECT l.id, a.area_code, a.area_name, a.description, 'ACTIVE'
FROM parking_lot l
CROSS JOIN (VALUES ('A', 'A区', '入口左侧'), ('B', 'B区', '入口右侧')) AS a (area_code, area_name, description);

-- ---------- 车位：每区域 001-010 ----------
INSERT INTO parking_space (area_id, space_code, description, status)
SELECT a.id, s.space_code, NULL, 'FREE'
FROM parking_area a
CROSS JOIN (VALUES ('001'), ('002'), ('003'), ('004'), ('005'),
                   ('006'), ('007'), ('008'), ('009'), ('010')) AS s (space_code);

-- ---------- 收费规则：每场一条生效规则（30 分钟免费，60 分钟 5 元，24 小时封顶 50 元） ----------
INSERT INTO fee_rule (parking_lot_id, rule_name, free_minutes, billing_unit_minutes,
                      unit_price, max_fee_per24h, is_active, effective_from, created_by)
SELECT l.id, '日常规则', 30, 60, 5.00, 50.00, true, now(),
       (SELECT id FROM user_account WHERE username = 'admin')
FROM parking_lot l;

-- ---------- 车主车辆 ----------
INSERT INTO vehicle (owner_user_id, license_plate, status)
SELECT u.id, v.plate, 'ACTIVE'
FROM user_account u
JOIN (VALUES ('owner1', '京A12345'), ('owner1', '京B66666'), ('owner2', '沪A88888')) AS v (username, plate)
    ON v.username = u.username;

-- ---------- 演示停车记录 ----------
-- 1) owner1 在京A12345 已完成的记录 + 正常收款（CASH 5.00）
WITH rec AS (
    INSERT INTO parking_record (record_no, parking_lot_id, parking_space_id, fee_rule_id, vehicle_id,
                                license_plate, entry_admin_id, entry_time, exit_admin_id, exit_time, status)
    SELECT 'R' || substr(replace(gen_random_uuid()::text, '-', ''), 1, 31),
           l.id, s.id, r.id, v.id, '京A12345',
           (SELECT id FROM user_account WHERE username = 'admin'),
           now() - interval '3 hours', (SELECT id FROM user_account WHERE username = 'admin'),
           now() - interval '2 hours', 'COMPLETED'
    FROM parking_lot l
    JOIN parking_area a ON a.parking_lot_id = l.id AND a.area_code = 'A'
    JOIN parking_space s ON s.area_id = a.id AND s.space_code = '002'
    JOIN fee_rule r ON r.parking_lot_id = l.id AND r.is_active
    JOIN vehicle v ON v.license_plate = '京A12345'
    WHERE l.lot_code = 'LOT-001'
    RETURNING id, entry_time, exit_time
), pay AS (
    INSERT INTO payment_record (payment_no, parking_record_id, amount_due, amount_paid,
                                payment_method, payment_time, remark, handled_by)
    SELECT 'P' || substr(replace(gen_random_uuid()::text, '-', ''), 1, 31),
           rec.id, 5.00, 5.00, 'CASH', rec.exit_time, NULL,
           (SELECT id FROM user_account WHERE username = 'admin')
    FROM rec
)
UPDATE parking_space SET status = 'OCCUPIED', updated_at = now()
WHERE id = (SELECT s.id FROM parking_lot l
            JOIN parking_area a ON a.parking_lot_id = l.id AND a.area_code = 'A'
            JOIN parking_space s ON s.area_id = a.id AND s.space_code = '002'
            WHERE l.lot_code = 'LOT-001');

-- 2) 一条临时车在场记录（未绑定车辆，演示异常/离场流程）
INSERT INTO parking_record (record_no, parking_lot_id, parking_space_id, fee_rule_id, vehicle_id,
                            license_plate, entry_admin_id, entry_time, status)
SELECT 'R' || substr(replace(gen_random_uuid()::text, '-', ''), 1, 31),
       l.id, s.id, r.id, NULL, '川B00001',
       (SELECT id FROM user_account WHERE username = 'admin'),
       now() - interval '45 minutes', 'PARKING'
FROM parking_lot l
JOIN parking_area a ON a.parking_lot_id = l.id AND a.area_code = 'B'
JOIN parking_space s ON s.area_id = a.id AND s.space_code = '003'
JOIN fee_rule r ON r.parking_lot_id = l.id AND r.is_active
WHERE l.lot_code = 'LOT-001';

UPDATE parking_space SET status = 'OCCUPIED', updated_at = now()
WHERE id = (SELECT s.id FROM parking_lot l
            JOIN parking_area a ON a.parking_lot_id = l.id AND a.area_code = 'B'
            JOIN parking_space s ON s.area_id = a.id AND s.space_code = '003'
            WHERE l.lot_code = 'LOT-001');
