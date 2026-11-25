package org.example.backend.service.fee;

/**
 * 收费规则只读查询契约（成员 3 提供给成员 2 的稳定内部接口，01 契约第 7 节）。
 * 只读、不新建规则；入场绑定所选场规则，无规则抛 4303。
 */
public interface FeeRuleQuery {

    /**
     * 取停车场当前生效规则快照（包含 feeRuleId，即规则主键 ID）。
     * 该停车场没有生效规则时抛 4303（NO_ACTIVE_FEE_RULE）。
     * 调用方（入场）应已在事务中通过 ParkingLotGuard 锁定并校验停车场，
     * 因此这里不再重复校验停车场存在性。
     */
    FeeRuleSnapshot findActiveForLot(Long lotId);

    /** 按规则 ID 取快照；不存在抛 4401 */
    FeeRuleSnapshot getSnapshotById(Long feeRuleId);

    /** 按规则 ID 取快照；不存在返回 null（记录查询派生字段容错用） */
    FeeRuleSnapshot findSnapshotByIdOrNull(Long feeRuleId);
}
