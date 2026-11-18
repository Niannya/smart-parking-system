package org.example.backend.common;

import org.example.backend.mapper.ParkingLotGuardMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在调用方已有事务中锁定停车场行；所有改变入场可用性的操作先调用此服务。
 * MANDATORY 防止锁在单独的短事务里提前释放。
 */
@Service
public class ParkingLotGuard {

    private final ParkingLotGuardMapper mapper;

    public ParkingLotGuard(ParkingLotGuardMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockLot(Long lotId) {
        lockStatus(lotId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockActiveLot(Long lotId) {
        if (!"ACTIVE".equals(lockStatus(lotId))) {
            throw new BusinessException(ErrorCode.LOT_DISABLED, "停车场已停用");
        }
    }

    private String lockStatus(Long lotId) {
        if (lotId == null || lotId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "停车场 ID 必须是正整数");
        }
        String status = mapper.selectStatusForUpdate(lotId);
        if (status == null) {
            throw new BusinessException(ErrorCode.LOT_NOT_FOUND, "停车场不存在");
        }
        return status;
    }
}
