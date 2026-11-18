package org.example.backend.common;

import org.springframework.http.HttpStatus;

/** docs/implementation/01_接口与数据契约.md 中的稳定业务错误码。 */
public enum ErrorCode {
    INVALID_REQUEST(4000, HttpStatus.BAD_REQUEST),
    UNKNOWN_ERROR(5000, HttpStatus.INTERNAL_SERVER_ERROR),

    BAD_CREDENTIALS(1001, HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(1002, HttpStatus.UNAUTHORIZED),
    UNAUTHENTICATED(1003, HttpStatus.UNAUTHORIZED),
    FORBIDDEN(1004, HttpStatus.FORBIDDEN),
    USERNAME_EXISTS(1005, HttpStatus.CONFLICT),
    INVALID_CSRF(1006, HttpStatus.FORBIDDEN),
    SELF_DISABLE_FORBIDDEN(1007, HttpStatus.CONFLICT),
    USER_NOT_FOUND(1008, HttpStatus.NOT_FOUND),

    LOT_NOT_FOUND(2001, HttpStatus.NOT_FOUND),
    LOT_CODE_EXISTS(2002, HttpStatus.CONFLICT),
    LOT_HAS_ACTIVE_RECORD(2003, HttpStatus.CONFLICT),
    LOT_HAS_DEPENDENCIES(2004, HttpStatus.CONFLICT),
    LOT_DISABLED(2005, HttpStatus.CONFLICT),

    AREA_NOT_FOUND(4001, HttpStatus.NOT_FOUND),
    AREA_CODE_EXISTS(4002, HttpStatus.CONFLICT),
    AREA_HAS_SPACES(4003, HttpStatus.CONFLICT),
    AREA_HAS_ACTIVE_RECORD(4004, HttpStatus.CONFLICT),
    AREA_DISABLED(4005, HttpStatus.CONFLICT),

    SPACE_NOT_FOUND(4101, HttpStatus.NOT_FOUND),
    SPACE_CODE_EXISTS(4102, HttpStatus.CONFLICT),
    SPACE_OCCUPIED(4103, HttpStatus.CONFLICT),
    SPACE_HAS_HISTORY(4104, HttpStatus.CONFLICT),
    SPACE_NOT_ALLOCATABLE(4105, HttpStatus.CONFLICT),

    VEHICLE_NOT_FOUND(4201, HttpStatus.NOT_FOUND),
    VEHICLE_PLATE_EXISTS(4202, HttpStatus.CONFLICT),
    VEHICLE_NOT_OWNED(4203, HttpStatus.FORBIDDEN),
    INVALID_LICENSE_PLATE(4204, HttpStatus.BAD_REQUEST),

    PLATE_ALREADY_PARKING(4301, HttpStatus.CONFLICT),
    NO_FREE_SPACE(4302, HttpStatus.CONFLICT),
    NO_ACTIVE_FEE_RULE(4303, HttpStatus.CONFLICT),
    ENTRY_CONFLICT(4304, HttpStatus.CONFLICT),

    FEE_RULE_NOT_FOUND(4401, HttpStatus.NOT_FOUND),
    INVALID_FEE_RULE(4402, HttpStatus.BAD_REQUEST),
    ACTIVE_FEE_RULE_NOT_FOUND(4403, HttpStatus.NOT_FOUND),

    ACTIVE_RECORD_NOT_FOUND(4501, HttpStatus.NOT_FOUND),
    RECORD_ALREADY_EXITED(4502, HttpStatus.CONFLICT),
    PAYMENT_AMOUNT_MISMATCH(4503, HttpStatus.CONFLICT),
    FREE_RELEASE_REASON_REQUIRED(4504, HttpStatus.CONFLICT),
    EXIT_CONFLICT(4505, HttpStatus.CONFLICT),

    RECORD_NOT_FOUND(4601, HttpStatus.NOT_FOUND),
    RECORD_NOT_OWNED(4602, HttpStatus.FORBIDDEN),
    INVALID_DATE_RANGE(4701, HttpStatus.BAD_REQUEST);

    private final int code;
    private final HttpStatus status;

    ErrorCode(int code, HttpStatus status) {
        this.code = code;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
