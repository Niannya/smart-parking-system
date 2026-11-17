package org.example.backend.service;

import org.springframework.stereotype.Component;

/**
 * 入场外部依赖的占位实现。
 *
 * <p>成员 1 的 {@code ParkingLotGuard} 和成员 3 的 {@code FeeRuleQuery} 尚未合并到
 * {@code dev}，本类让项目仍可编译、启动，并让入场接口明确返回“依赖未合并”，
 * 而不是偷偷在 {@code ParkingEntryMapper} 里复制一份锁场/取规则 SQL。</p>
 *
 * <p>合并后：新增一个 {@code @Primary} 的 {@link ParkingEntryDependency} 实现，
 * 注入 {@code ParkingLotGuard} 和 {@code FeeRuleQuery}，然后删除本类（或去掉
 * {@code @Component}）。操作步骤见 {@code docs/成员2_交付与合并操作说明.md}。</p>
 */
@Component
public class PendingParkingEntryDependency implements ParkingEntryDependency {

    public static final String NOT_READY_MESSAGE =
            "入场依赖尚未合并：请先合并成员 1 的 ParkingLotGuard 与成员 3 的 FeeRuleQuery，"
                    + "并按 docs/成员2_交付与合并操作说明.md 替换 PendingParkingEntryDependency";

    @Override
    public void lockActiveLot(Long lotId) {
        throw new IllegalStateException(NOT_READY_MESSAGE);
    }

    @Override
    public Long findActiveFeeRuleId(Long lotId) {
        throw new IllegalStateException(NOT_READY_MESSAGE);
    }
}
