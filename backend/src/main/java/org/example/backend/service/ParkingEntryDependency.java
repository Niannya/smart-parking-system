package org.example.backend.service;

/**
 * 入场事务的外部依赖端口（成员 2 只声明端口，不复制成员 1/3 的实现）。
 *
 * <p>正式实现必须由合并后的公共模块提供：</p>
 * <ul>
 *   <li>{@link #lockActiveLot(Long)} 委托成员 1 的 {@code ParkingLotGuard.lockActiveLot(lotId)}，
 *       必须在调用方当前事务内执行 {@code SELECT ... FOR UPDATE}，不能独立提交；
 *       停车场不存在抛 {@code BusinessException(2001)}，已停用抛 {@code BusinessException(2005)}。</li>
 *   <li>{@link #findActiveFeeRuleId(Long)} 委托成员 3 的
 *       {@code FeeRuleQuery.findActiveForLot(lotId)}，只读；没有生效规则返回 {@code null}，
 *       由入场服务抛 {@code BusinessException(4303)}。</li>
 * </ul>
 *
 * <p>当前成员 1/3 的类尚未合并到 {@code dev}，占位实现见
 * {@link PendingParkingEntryDependency}；合并后按
 * {@code docs/成员2_交付与合并操作说明.md} 替换即可，不需要改本接口和入场服务。</p>
 */
public interface ParkingEntryDependency {

    /**
     * 在当前事务内锁定并校验停车场为 ACTIVE。
     *
     * @param lotId 停车场 ID，调用方已保证为正整数
     */
    void lockActiveLot(Long lotId);

    /**
     * 返回该停车场当前生效的收费规则 ID；没有生效规则时返回 {@code null}。
     *
     * @param lotId 停车场 ID，调用方已保证为正整数
     */
    Long findActiveFeeRuleId(Long lotId);
}
