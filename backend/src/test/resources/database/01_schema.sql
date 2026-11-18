-- =============================================================
-- 智能停车场管理系统 数据库结构（PostgreSQL）
-- 依据 docs/implementation/00_工程实施规格.md 与 01_接口与数据契约.md
-- 约定：所有业务表均带明确停车场归属；TIMESTAMPTZ 不靠字符串比较；
--       金额 NUMERIC(10,2) 对应 Java BigDecimal。
-- =============================================================

-- 密码哈希使用 pgcrypto 的 bcrypt（演示数据由 02_seed_data.sql 生成）
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------- 1. 用户账号 ----------
CREATE TABLE user_account (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'OWNER'
                  CHECK (role IN ('ADMIN', 'OWNER')),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------- 2. 停车场 ----------
CREATE TABLE parking_lot (
    id          BIGSERIAL    PRIMARY KEY,
    lot_code    VARCHAR(32)  NOT NULL UNIQUE,
    lot_name    VARCHAR(100) NOT NULL,
    address     VARCHAR(200),
    description VARCHAR(500),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------- 3. 停车区域（归属停车场） ----------
CREATE TABLE parking_area (
    id             BIGSERIAL    PRIMARY KEY,
    parking_lot_id BIGINT       NOT NULL REFERENCES parking_lot (id),
    area_code      VARCHAR(32)  NOT NULL,
    area_name      VARCHAR(100) NOT NULL,
    description    VARCHAR(500),
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                   CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_parking_area_lot_code UNIQUE (parking_lot_id, area_code)
);

-- ---------- 4. 停车位（归属区域，经区域归属停车场） ----------
CREATE TABLE parking_space (
    id          BIGSERIAL    PRIMARY KEY,
    area_id     BIGINT       NOT NULL REFERENCES parking_area (id),
    space_code  VARCHAR(32)  NOT NULL,
    description VARCHAR(500),
    status      VARCHAR(20)  NOT NULL DEFAULT 'FREE'
                CHECK (status IN ('FREE', 'OCCUPIED', 'DISABLED')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_parking_space_area_code UNIQUE (area_id, space_code)
);

-- ---------- 5. 车主车辆（全局车牌唯一，归属车主） ----------
CREATE TABLE vehicle (
    id             BIGSERIAL    PRIMARY KEY,
    owner_user_id  BIGINT       NOT NULL REFERENCES user_account (id),
    license_plate  VARCHAR(15)  NOT NULL UNIQUE,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                   CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------- 6. 收费规则（每场历史保留，同场最多一条活动规则） ----------
CREATE TABLE fee_rule (
    id                    BIGSERIAL      PRIMARY KEY,
    parking_lot_id        BIGINT         NOT NULL REFERENCES parking_lot (id),
    rule_name             VARCHAR(100)   NOT NULL,
    free_minutes          INTEGER        NOT NULL CHECK (free_minutes >= 0),
    billing_unit_minutes  INTEGER        NOT NULL CHECK (billing_unit_minutes > 0),
    unit_price            NUMERIC(10, 2) NOT NULL CHECK (unit_price >= 0),
    max_fee_per24h        NUMERIC(10, 2) NOT NULL CHECK (max_fee_per24h >= 0),
    is_active             BOOLEAN        NOT NULL DEFAULT true,
    effective_from        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    effective_to          TIMESTAMPTZ,
    created_by            BIGINT         NOT NULL REFERENCES user_account (id),
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now()
);
-- 同一停车场最多一条生效规则（并发切换时由数据库兜底）
CREATE UNIQUE INDEX uk_fee_rule_active_lot ON fee_rule (parking_lot_id) WHERE is_active;

-- ---------- 7. 停车记录（入场时绑定当时有效规则，保留车牌快照） ----------
CREATE TABLE parking_record (
    id               BIGSERIAL    PRIMARY KEY,
    record_no        VARCHAR(32)  NOT NULL UNIQUE,
    parking_lot_id   BIGINT       NOT NULL REFERENCES parking_lot (id),
    parking_space_id BIGINT       NOT NULL REFERENCES parking_space (id),
    fee_rule_id      BIGINT       NOT NULL REFERENCES fee_rule (id),
    vehicle_id       BIGINT       REFERENCES vehicle (id),
    license_plate    VARCHAR(15)  NOT NULL,
    entry_admin_id   BIGINT       NOT NULL REFERENCES user_account (id),
    entry_time       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    exit_admin_id    BIGINT       REFERENCES user_account (id),
    exit_time        TIMESTAMPTZ,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PARKING'
                     CHECK (status IN ('PARKING', 'COMPLETED', 'ABNORMAL')),
    abnormal_reason  VARCHAR(500),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- 全局同一车牌最多一条在场记录（并发入场兜底）
CREATE UNIQUE INDEX uk_parking_record_active_plate ON parking_record (license_plate) WHERE status = 'PARKING';
-- 同一车位最多一条在场记录（并发占位兜底）
CREATE UNIQUE INDEX uk_parking_record_active_space ON parking_record (parking_space_id) WHERE status = 'PARKING';

-- ---------- 8. 支付记录（一条停车记录最多一笔支付） ----------
CREATE TABLE payment_record (
    id                BIGSERIAL      PRIMARY KEY,
    payment_no        VARCHAR(32)    NOT NULL UNIQUE,
    parking_record_id BIGINT         NOT NULL UNIQUE REFERENCES parking_record (id),
    amount_due        NUMERIC(10, 2) NOT NULL CHECK (amount_due >= 0),
    amount_paid       NUMERIC(10, 2) NOT NULL CHECK (amount_paid >= 0),
    payment_method    VARCHAR(20)    NOT NULL
                      CHECK (payment_method IN ('CASH', 'ELECTRONIC', 'FREE_RELEASE')),
    payment_time      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    remark            VARCHAR(500),
    handled_by        BIGINT         NOT NULL REFERENCES user_account (id),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- 常用查询索引
CREATE INDEX idx_parking_area_lot    ON parking_area (parking_lot_id);
CREATE INDEX idx_parking_space_area  ON parking_space (area_id);
CREATE INDEX idx_vehicle_owner       ON vehicle (owner_user_id);
CREATE INDEX idx_fee_rule_lot        ON fee_rule (parking_lot_id, created_at DESC, id DESC);
CREATE INDEX idx_parking_record_lot  ON parking_record (parking_lot_id, entry_time DESC, id DESC);
CREATE INDEX idx_payment_time        ON payment_record (payment_time);
