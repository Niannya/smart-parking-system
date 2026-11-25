package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.common.CurrentUser;
import org.example.backend.common.ErrorCode;
import org.example.backend.common.ParkingLotGuard;
import org.example.backend.dto.FeeRuleCreateRequest;
import org.example.backend.entity.FeeRule;
import org.example.backend.mapper.FeeRuleMapper;
import org.example.backend.mapper.ParkingLotMapper;
import org.example.backend.service.fee.FeeRuleQuery;
import org.example.backend.service.fee.FeeRuleSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 收费规则管理（成员 3，M2-05/06）。
 *
 * <p>业务规则来源：01 契约第 4 节、数据字典 fee_rule。
 * 关键几点：
 * 1. 每场规则历史保留；新建规则在同一事务内先锁启用停车场，关闭旧活动规则
 *    （is_active=false, effective_to=now），再插新规则（is_active=true, effective_from=now）；
 * 2. createdBy 取当前登录账号（CurrentUser），路径 /api/fee-rules/** 仅 ADMIN；
 * 3. 历史规则不可改删，停车记录保留入场时绑定的 feeRuleId，因此没有 PUT/DELETE；
 * 4. rule_name 上限 50 字符，与 fee_rule.rule_name VARCHAR(50) 保持一致。</p>
 */
@Service
public class FeeRuleService implements FeeRuleQuery {

    /** fee_rule.rule_name VARCHAR(50) */
    private static final int RULE_NAME_MAX_LENGTH = 50;
    /** fee_rule 金额列为 NUMERIC(10, 2)：最多 8 位整数、2 位小数 */
    private static final int MONEY_MAX_INTEGER_DIGITS = 8;
    private static final int MONEY_MAX_FRACTION_DIGITS = 2;

    private final FeeRuleMapper feeRuleMapper;
    private final ParkingLotMapper lots;
    private final ParkingLotGuard guard;

    public FeeRuleService(FeeRuleMapper feeRuleMapper, ParkingLotMapper lots, ParkingLotGuard guard) {
        this.feeRuleMapper = feeRuleMapper;
        this.lots = lots;
        this.guard = guard;
    }

    /** GET /api/fee-rules?lotId=：按 createdAt DESC, id DESC */
    @Transactional(readOnly = true)
    public List<FeeRule> listByLot(Long lotId) {
        requireLotId(lotId);
        requireLotExists(lotId);
        return feeRuleMapper.selectByLot(lotId);
    }

    /** GET /api/fee-rules/active?lotId=：无规则 404/4403 */
    @Transactional(readOnly = true)
    public FeeRule getActive(Long lotId) {
        requireLotId(lotId);
        requireLotExists(lotId);
        FeeRule rule = feeRuleMapper.selectActive(lotId);
        if (rule == null) {
            throw new BusinessException(ErrorCode.ACTIVE_FEE_RULE_NOT_FOUND, "当前停车场没有生效的收费规则");
        }
        return rule;
    }

    /** POST /api/fee-rules：创建即激活，旧规则转历史 */
    @Transactional(rollbackFor = Exception.class)
    public FeeRule create(FeeRuleCreateRequest request) {
        Long lotId = requireLotId(request.getLotId());
        String ruleName = requiredText(request.getRuleName(), "规则名称", RULE_NAME_MAX_LENGTH);
        // 免费时长/计费单位经 DTO 校验：拒绝小数（Jackson 不会替我们拒绝）、空值与超范围
        int freeMinutes = request.requireFreeMinutes(0);
        int billingUnitMinutes = request.requireBillingUnitMinutes(1);
        BigDecimal unitPrice = requiredMoney(request.getUnitPrice(), "单位价格");
        BigDecimal maxFeePer24h = requiredMoney(request.getMaxFeePer24h(), "24 小时封顶费");

        // 同一事务锁启用停车场：不存在 2001、已停用 2005；锁顺序与区域/车位/规则切换/入场一致
        guard.lockActiveLot(lotId);

        OffsetDateTime now = OffsetDateTime.now();
        feeRuleMapper.deactivateActive(lotId, now);

        FeeRule rule = new FeeRule();
        rule.setParkingLotId(lotId);
        rule.setRuleName(ruleName);
        rule.setFreeMinutes(freeMinutes);
        rule.setBillingUnitMinutes(billingUnitMinutes);
        rule.setUnitPrice(unitPrice.setScale(MONEY_MAX_FRACTION_DIGITS, RoundingMode.HALF_UP));
        rule.setMaxFeePer24h(maxFeePer24h.setScale(MONEY_MAX_FRACTION_DIGITS, RoundingMode.HALF_UP));
        rule.setIsActive(true);
        rule.setEffectiveFrom(now);
        rule.setEffectiveTo(null);
        rule.setCreatedBy(CurrentUser.id());
        feeRuleMapper.insert(rule);
        return feeRuleMapper.selectById(rule.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public FeeRuleSnapshot findActiveForLot(Long lotId) {
        FeeRule rule = feeRuleMapper.selectActive(lotId);
        if (rule == null) {
            throw new BusinessException(ErrorCode.NO_ACTIVE_FEE_RULE, "当前停车场没有生效收费规则");
        }
        return toSnapshot(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public FeeRuleSnapshot getSnapshotById(Long feeRuleId) {
        FeeRuleSnapshot snapshot = findSnapshotByIdOrNull(feeRuleId);
        if (snapshot == null) {
            throw new BusinessException(ErrorCode.FEE_RULE_NOT_FOUND, "收费规则不存在");
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

    private static Long requireLotId(Long lotId) {
        if (lotId == null || lotId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "停车场 ID 必须是正整数");
        }
        return lotId;
    }

    private void requireLotExists(Long lotId) {
        // 停车场存在性经成员 1 的只读 Mapper 判定，不在本模块复制查询
        if (lots.selectById(lotId) == null) {
            throw new BusinessException(ErrorCode.LOT_NOT_FOUND, "停车场不存在");
        }
    }

    private static String requiredText(String input, String label, int maxLength) {
        if (input == null || input.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_FEE_RULE, label + "不能为空");
        }
        String text = input.strip();
        if (text.indexOf(0) >= 0 || text.codePointCount(0, text.length()) > maxLength) {
            throw new BusinessException(ErrorCode.INVALID_FEE_RULE,
                    label + "包含无效字符或超过 " + maxLength + " 个字符");
        }
        return text;
    }

    private static BigDecimal requiredMoney(BigDecimal value, String label) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_FEE_RULE, label + "不能为空");
        }
        if (value.signum() < 0) {
            throw new BusinessException(ErrorCode.INVALID_FEE_RULE, label + "不能为负");
        }
        if (value.scale() > MONEY_MAX_FRACTION_DIGITS
                || value.precision() - value.scale() > MONEY_MAX_INTEGER_DIGITS) {
            throw new BusinessException(ErrorCode.INVALID_FEE_RULE,
                    label + "最多 " + MONEY_MAX_INTEGER_DIGITS + " 位整数、"
                            + MONEY_MAX_FRACTION_DIGITS + " 位小数");
        }
        return value;
    }
}
