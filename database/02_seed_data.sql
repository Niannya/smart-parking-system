-- 智能停车场管理系统：演示数据
-- 可重复执行；只使用虚构账号、车牌和地址。

BEGIN;

-- 使用 PostgreSQL 自带扩展生成 BCrypt 密码摘要。
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO user_account (username, password_hash, display_name, role, status)
VALUES
    ('admin', crypt('Demo123456', gen_salt('bf', 10)), '系统管理员', 'ADMIN', 'ACTIVE'),
    ('owner_demo', crypt('Demo123456', gen_salt('bf', 10)), '演示车主', 'OWNER', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

INSERT INTO vehicle (owner_user_id, license_plate, status)
SELECT id, '京A12345', 'ACTIVE'
FROM user_account
WHERE username = 'owner_demo'
ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO parking_lot (lot_code, lot_name, address, description, status)
VALUES
    ('LOT-001', '中心停车场', '示例市中心路 1 号', '用于演示多停车场管理', 'ACTIVE'),
    ('LOT-002', '校园停车场', '示例市大学路 2 号', '用于演示停车场切换', 'ACTIVE')
ON CONFLICT (lot_code) DO NOTHING;

INSERT INTO parking_area (parking_lot_id, area_code, area_name, description, status)
SELECT lot.id, area.area_code, area.area_name, area.description, 'ACTIVE'
FROM parking_lot AS lot
CROSS JOIN (
    VALUES
        ('A', 'A 区', '靠近入口'),
        ('B', 'B 区', '靠近出口')
) AS area(area_code, area_name, description)
WHERE lot.lot_code IN ('LOT-001', 'LOT-002')
ON CONFLICT (parking_lot_id, area_code) DO NOTHING;

INSERT INTO parking_space (area_id, space_code, status)
SELECT area.id, LPAD(number::TEXT, 3, '0'), 'FREE'
FROM parking_area AS area
CROSS JOIN GENERATE_SERIES(1, 10) AS series(number)
JOIN parking_lot AS lot ON lot.id = area.parking_lot_id
WHERE lot.lot_code IN ('LOT-001', 'LOT-002')
ON CONFLICT (area_id, space_code) DO NOTHING;

INSERT INTO fee_rule (
    parking_lot_id,
    rule_name,
    free_minutes,
    billing_unit_minutes,
    unit_price,
    max_fee_per_24h,
    is_active,
    effective_from,
    created_by
)
SELECT
    lot.id,
    '默认收费规则',
    30,
    60,
    CASE lot.lot_code WHEN 'LOT-001' THEN 5.00 ELSE 4.00 END,
    CASE lot.lot_code WHEN 'LOT-001' THEN 50.00 ELSE 40.00 END,
    TRUE,
    TIMESTAMPTZ '2026-01-01 00:00:00+08',
    admin_user.id
FROM parking_lot AS lot
CROSS JOIN (
    SELECT id
    FROM user_account
    WHERE username = 'admin'
) AS admin_user
WHERE lot.lot_code IN ('LOT-001', 'LOT-002')
  AND NOT EXISTS (
      SELECT 1
      FROM fee_rule AS existing
      WHERE existing.parking_lot_id = lot.id
        AND existing.is_active
  );

COMMIT;
