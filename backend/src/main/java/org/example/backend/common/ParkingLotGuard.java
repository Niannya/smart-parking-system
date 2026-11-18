package org.example.backend.common;

import lombok.RequiredArgsConstructor;
import org.example.backend.mapper.ParkingLotMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 停车场行锁（成员 1 提供给 2/3 的公共契约，00 规格第 6 节）。
 * 固定顺序：先锁停车场行，再读/锁区域、车位、规则或在场记录。
 * 锁依赖当前事务：没有活动事务时调用属于误用，直接拒绝，避免锁独立提交。
 */
@Component
@RequiredArgsConstructor
public class ParkingLotGuard {

    private final ParkingLotMapper parkingLotMapper;

    /** 锁停车场行；不存在抛 404/2001 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockLot(Long lotId) {
        requireActiveTransaction();
        Integer locked = parkingLotMapper.lockById(lotId);
        if (locked == null) {
            throw BusinessException.notFound(ErrorCodes.LOT_NOT_FOUND, "停车场不存在");
        }
    }

    /** 锁停车场行并校验 ACTIVE；已停用抛 409/2005 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockActiveLot(Long lotId) {
        lockLot(lotId);
        String status = parkingLotMapper.selectStatusById(lotId);
        if (!"ACTIVE".equals(status)) {
            throw BusinessException.conflict(ErrorCodes.LOT_DISABLED, "停车场已停用");
        }
    }

    private void requireActiveTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("ParkingLotGuard 必须在业务事务内调用（依赖当前事务，不独立提交）");
        }
    }
}
