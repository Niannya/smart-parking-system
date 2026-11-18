package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCode;
import org.example.backend.common.ParkingLotGuard;
import org.example.backend.dto.ParkingLotDto;
import org.example.backend.mapper.ParkingLotMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ParkingLotService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DISABLED = "DISABLED";

    private final ParkingLotMapper lots;
    private final ParkingLotGuard guard;

    public ParkingLotService(ParkingLotMapper lots, ParkingLotGuard guard) {
        this.lots = lots;
        this.guard = guard;
    }

    public List<ParkingLotDto.View> list(String status) {
        if (status != null) {
            requireStatus(status);
        }
        return lots.selectLots(status);
    }

    public List<ParkingLotDto.Option> options() {
        return lots.selectActiveOptions();
    }

    @Transactional
    public ParkingLotDto.View create(ParkingLotDto.CreateRequest request) {
        String lotCode = request.lotCode() == null ? null : request.lotCode().toUpperCase(Locale.ROOT);
        lotCode = requiredText(lotCode, "停车场编号", 30);
        String lotName = requiredText(request.lotName(), "停车场名称", 100);
        String address = requiredText(request.address(), "地址", 255);
        String description = description(request.description());

        if (lots.insert(lotCode, lotName, address, description) == 0) {
            throw new BusinessException(ErrorCode.LOT_CODE_EXISTS, "停车场编号已存在");
        }
        return lots.selectByCode(lotCode);
    }

    @Transactional
    public ParkingLotDto.View update(Long id, ParkingLotDto.UpdateRequest request) {
        String lotName = requiredText(request.lotName(), "停车场名称", 100);
        String address = requiredText(request.address(), "地址", 255);
        String description = description(request.description());
        guard.lockLot(id);
        lots.updateInfo(id, lotName, address, description);
        return lots.selectById(id);
    }

    @Transactional
    public ParkingLotDto.View changeStatus(Long id, String status) {
        requireStatus(status);
        // 与区域/车位变更、规则切换和入场使用同一个锁顺序。
        guard.lockLot(id);
        if (DISABLED.equals(status) && lots.hasActiveRecords(id)) {
            throw new BusinessException(ErrorCode.LOT_HAS_ACTIVE_RECORD, "有车辆仍在场，不能停用停车场");
        }
        lots.updateStatus(id, status);
        return lots.selectById(id);
    }

    @Transactional
    public void delete(Long id) {
        guard.lockLot(id);
        if (lots.hasDependencies(id)) {
            throw dependencyConflict();
        }
        try {
            lots.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            // 已有数据库的外键约束仍负责最终保护。
            throw dependencyConflict();
        }
    }

    private static BusinessException dependencyConflict() {
        return new BusinessException(ErrorCode.LOT_HAS_DEPENDENCIES, "停车场仍有关联区域、收费规则或停车记录，不能删除");
    }

    private static void requireStatus(String status) {
        if (!ACTIVE.equals(status) && !DISABLED.equals(status)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "状态只能是 ACTIVE 或 DISABLED");
        }
    }

    private static String requiredText(String input, String label, int maxLength) {
        if (input == null || input.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, label + "不能为空");
        }
        return limitedText(input, label, maxLength);
    }

    private static String description(String value) {
        return value == null || value.isBlank() ? null : limitedText(value, "说明", 500);
    }

    private static String limitedText(String input, String label, int maxLength) {
        String text = input.strip();
        if (text.indexOf(0) >= 0 || text.codePointCount(0, text.length()) > maxLength) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, label + "包含无效字符或超过 " + maxLength + " 字符");
        }
        return text;
    }
}
