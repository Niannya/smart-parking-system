package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.BusinessException;
import org.example.backend.common.CurrentUser;
import org.example.backend.common.ErrorCodes;
import org.example.backend.common.ParkingLotGuard;
import org.example.backend.dto.feerule.FeeRule;
import org.example.backend.dto.feerule.FeeRuleCreateRequest;
import org.example.backend.mapper.FeeRuleMapper;
import org.example.backend.mapper.ParkingLotMapper;
import org.example.backend.service.fee.FeeRuleQuery;
import org.example.backend.service.fee.FeeRuleSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 收费规则管理（成员 3，M2-05/06）。
 * 每场规则历史保留；新建规则在同一事务内先锁启用停车场，
 * 关闭旧活动规则（is_active=false, effective_to=now），再插新规则（is_active=true, effective_from=now）。
 * createdBy 来自当前 ADMIN；无 PUT/DELETE。
 */
@Service
@RequiredArgsConstructor
public class FeeRuleService implements FeeRuleQuery {

    private final FeeRuleMapper feeRuleMapper;
    private final ParkingLotMapper parkingLotMapper;
    private final ParkingLotGuard parkingLotGuard;
    private final CurrentUser currentUser;

    /** GET /api/fee-rules?lotId=：按 createdAt DESC, id DESC */
    @Transactional(readOnly = true)
    public List<FeeRule> listByLot(Long lotId) {
        requireLotExists(lotId);
        return feeRuleMapper.selectByLot(lotId);
    }

    /** GET /api/fee-rules/active?lotId=：无规则 404/4403 */
    @Transactional(readOnly = true)
    public FeeRule getActive(Long lotId) {
        requireLotExists(lotId);
        FeeRule rule = feeRuleMapper.selectActive(lotId);
        if (rule == null) {
            throw BusinessException.notFound(ErrorCodes.FEE_RULE_ACTIVE_NOT_FOUND, "当前停车场没有生效的收费规则");
        }
        return rule;
    }

    /** POST /api/fee-rules：创建即激活，旧规则转历史 */
    @Transactional(rollbackFor = Exception.class)
    public FeeRule create(FeeRuleCreateRequest request) {
        Long lotId = request.getLotId();
        parkingLotGuard.lockActiveLot(lotId);
        OffsetDateTime now = OffsetDateTime.now();
        feeRuleMapper.deactivateActive(lotId, now);

        FeeRule rule = new FeeRule();
        rule.setParkingLotId(lotId);
        rule.setRuleName(request.getRuleName().trim());
        rule.setFreeMinutes(request.getFreeMinutes());
        rule.setBillingUnitMinutes(request.getBillingUnitMinutes());
        rule.setUnitPrice(request.getUnitPrice().setScale(2, java.math.RoundingMode.HALF_UP));
        rule.setMaxFeePer24h(request.getMaxFeePer24h().setScale(2, java.math.RoundingMode.HALF_UP));
        rule.setIsActive(true);
        rule.setEffectiveFrom(now);
        rule.setEffectiveTo(null);
        rule.setCreatedBy(currentUser.requireAdminId());
        feeRuleMapper.insert(rule);
        return feeRuleMapper.selectById(rule.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public FeeRuleSnapshot findActiveForLot(Long lotId) {
        FeeRule rule = feeRuleMapper.selectActive(lotId);
        if (rule == null) {
            throw BusinessException.conflict(ErrorCodes.ENTRY_NO_ACTIVE_RULE, "当前停车场没有生效收费规则");
        }
        return toSnapshot(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public FeeRuleSnapshot getSnapshotById(Long feeRuleId) {
        FeeRuleSnapshot snapshot = findSnapshotByIdOrNull(feeRuleId);
        if (snapshot == null) {
            throw BusinessException.notFound(ErrorCodes.FEE_RULE_NOT_FOUND, "收费规则不存在");
        }
        return snapshot;
    }

    @Override
    @Transactional(readOnly = true)
    public FeeRuleSnapshot findSnapshotByIdOrNull(Long feeRuleId) {
        FeeRule rule = feeRuleMapper.selectById(feeRuleId);
        return rule == null ? null : toSnapshot(rule);
    }

    private FeeRuleSnapshot toSnapshot(FeeRule rule) {
        return new FeeRuleSnapshot(rule.getId(), rule.getRuleName(), rule.getFreeMinutes(),
                rule.getBillingUnitMinutes(), rule.getUnitPrice(), rule.getMaxFeePer24h());
    }

    private void requireLotExists(Long lotId) {
        // 停车场存在性经成员 1 的只读 Mapper 判定，不在本模块复制查询
        if (parkingLotMapper.selectStatusById(lotId) == null) {
            throw BusinessException.notFound(ErrorCodes.LOT_NOT_FOUND, "停车场不存在");
        }
    }
}
