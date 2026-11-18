package org.example.backend.common;

/**
 * 稳定业务错误码（01 接口契约第 1 节“稳定错误码”表）。
 * 同一分段内由负责者登记，不能重复占号。
 */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    // 通用
    public static final int BAD_REQUEST = 4000;
    public static final int UNKNOWN = 5000;

    // 认证 1001-1099
    public static final int AUTH_BAD_CREDENTIALS = 1001;
    public static final int AUTH_ACCOUNT_DISABLED = 1002;
    public static final int AUTH_UNAUTHORIZED = 1003;
    public static final int AUTH_FORBIDDEN = 1004;
    public static final int AUTH_USERNAME_EXISTS = 1005;
    public static final int AUTH_CSRF_INVALID = 1006;
    public static final int AUTH_SELF_DISABLE = 1007;

    // 停车场 2001-2099
    public static final int LOT_NOT_FOUND = 2001;
    public static final int LOT_CODE_DUPLICATE = 2002;
    public static final int LOT_HAS_PARKING = 2003;
    public static final int LOT_HAS_REFERENCE = 2004;
    public static final int LOT_DISABLED = 2005;

    // 区域 4001-4099
    public static final int AREA_NOT_FOUND = 4001;
    public static final int AREA_CODE_DUPLICATE = 4002;

    // 车位 4101-4199
    public static final int SPACE_NOT_FOUND = 4101;

    // 车辆 4201-4299
    public static final int VEHICLE_NOT_FOUND = 4201;
    public static final int VEHICLE_PLATE_DUPLICATE = 4202;
    public static final int VEHICLE_NOT_OWNED = 4203;
    public static final int VEHICLE_PLATE_INVALID = 4204;

    // 入场 4301-4399
    public static final int ENTRY_PLATE_ACTIVE = 4301;
    public static final int ENTRY_NO_FREE_SPACE = 4302;
    public static final int ENTRY_NO_ACTIVE_RULE = 4303;
    public static final int ENTRY_CONCURRENT_CONFLICT = 4304;

    // 收费规则 4401-4499
    public static final int FEE_RULE_NOT_FOUND = 4401;
    public static final int FEE_RULE_PARAM_INVALID = 4402;
    public static final int FEE_RULE_ACTIVE_NOT_FOUND = 4403;

    // 离场/支付 4501-4599
    public static final int EXIT_NO_ACTIVE_RECORD = 4501;
    public static final int EXIT_ALREADY_COMPLETED = 4502;
    public static final int EXIT_AMOUNT_MISMATCH = 4503;
    public static final int EXIT_FREE_RELEASE_REASON_REQUIRED = 4504;
    public static final int EXIT_CONCURRENT_CONFLICT = 4505;

    // 记录查询 4601-4699
    public static final int RECORD_NOT_FOUND = 4601;
    public static final int RECORD_NOT_OWNED = 4602;

    // 统计 4701-4799
    public static final int DATE_RANGE_INVALID = 4701;
}
