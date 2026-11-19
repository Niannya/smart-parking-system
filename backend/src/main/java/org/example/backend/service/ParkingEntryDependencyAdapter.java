package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCode;
import org.example.backend.common.ParkingLotGuard;
import org.springframework.stereotype.Component;

/**
 * 入场依赖适配器。
 *
 * <p>成员 1 的 {@link ParkingLotGuard} 已合并到 {@code dev}，这里直接委托它锁场；
 * 成员 3 的 {@code FeeRuleQuery} 尚未合并，取规则方法暂时明确报“收费规则接口未接入”，
 * 不在入场 Mapper 里复制规则查询 SQL。</p>
 *
 * <p>成员 3 合并后：在本类注入 {@code FeeRuleQuery}，把 {@link #findActiveFeeRuleId(Long)}
 * 改成委托调用并返回规则 ID；详细步骤见 {@code docs/成员2_交付与合并操作说明.md}。</p>
 */
@Component
public class ParkingEntryDependencyAdapter implements ParkingEntryDependency {

    public static final String FEE_RULE_NOT_READY_MESSAGE =
            "收费规则接口尚未合并：请先合并成员 3 的 FeeRuleQuery，并接入 "
                    + "ParkingEntryDependencyAdapter.findActiveFeeRuleId";

    private final ParkingLotGuard parkingLotGuard;

    public ParkingEntryDependencyAdapter(ParkingLotGuard parkingLotGuard) {
        this.parkingLotGuard = parkingLotGuard;
    }

    @Override
    public void lockActiveLot(Long lotId) {
        parkingLotGuard.lockActiveLot(lotId);
    }

    @Override
    public Long findActiveFeeRuleId(Long lotId) {
        throw new BusinessException(ErrorCode.UNKNOWN_ERROR, FEE_RULE_NOT_READY_MESSAGE);
    }
}
