package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.dto.ParkingAreaSaveRequest;
import org.example.backend.dto.ParkingLotOption;
import org.example.backend.entity.ParkingArea;
import org.example.backend.mapper.ParkingAreaMapper;
import org.example.backend.support.TextNormalizer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 停车区域业务。
 *
 * <p>业务规则来源：需求分析 FR-AREA-01~05、数据字典 6.4。
 * 关键几点：
 * 1. 区域编号在同一停车场内唯一，先查后插，数据库唯一约束作为并发兜底；
 * 2. 编号入库前必须去空格转大写，否则违反数据库 CHECK 约束；
 * 3. 区域下有车位不能删除（FR-AREA-04）；
 * 4. 区域下有在场车辆不能停用（数据字典 6.4）。</p>
 */
@Service
public class ParkingAreaService {

    private static final int CODE_MAX_LENGTH = 20;
    private static final int NAME_MAX_LENGTH = 50;
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    private final ParkingAreaMapper parkingAreaMapper;

    public ParkingAreaService(ParkingAreaMapper parkingAreaMapper) {
        this.parkingAreaMapper = parkingAreaMapper;
    }

    public List<ParkingArea> list(Long lotId, String status) {
        if (lotId == null) {
            throw new BusinessException(4001, "请先选择停车场");
        }
        String normalizedStatus = status == null || status.isBlank() ? null : parseStatus(status);
        return parkingAreaMapper.selectByLot(lotId, normalizedStatus);
    }

    public List<ParkingLotOption> listLotOptions() {
        return parkingAreaMapper.selectActiveLots();
    }

    @Transactional(rollbackFor = Exception.class)
    public ParkingArea create(ParkingAreaSaveRequest request) {
        if (request.getLotId() == null) {
            throw new BusinessException(4001, "请先选择停车场");
        }

        String areaCode = TextNormalizer.normalizeCode(request.getAreaCode());
        if (areaCode.isEmpty()) {
            throw new BusinessException(4001, "区域编号不能为空");
        }
        if (areaCode.length() > CODE_MAX_LENGTH) {
            throw new BusinessException(4001, "区域编号不能超过 " + CODE_MAX_LENGTH + " 个字符");
        }

        String areaName = TextNormalizer.normalizeText(request.getAreaName());
        if (areaName.isEmpty()) {
            throw new BusinessException(4001, "区域名称不能为空");
        }
        if (areaName.length() > NAME_MAX_LENGTH) {
            throw new BusinessException(4001, "区域名称不能超过 " + NAME_MAX_LENGTH + " 个字符");
        }

        String description = normalizeDescription(request.getDescription());

        // 停车场必须存在且处于启用状态，停用停车场不允许再维护区域
        String lotStatus = parkingAreaMapper.selectLotStatus(request.getLotId());
        if (lotStatus == null) {
            throw new BusinessException(4004, "停车场不存在");
        }
        if (!"ACTIVE".equals(lotStatus)) {
            throw new BusinessException(4005, "该停车场已停用，不能新增区域");
        }

        if (parkingAreaMapper.countByLotAndCode(request.getLotId(), areaCode) > 0) {
            throw new BusinessException(4002, "该停车场已存在区域编号 " + areaCode);
        }

        ParkingArea area = new ParkingArea();
        area.setParkingLotId(request.getLotId());
        area.setAreaCode(areaCode);
        area.setAreaName(areaName);
        area.setDescription(description);
        try {
            parkingAreaMapper.insert(area);
        } catch (DuplicateKeyException e) {
            // 并发下两个请求同时通过上面的查重，由 uk_parking_area_lot_code 拦下第二个
            throw new BusinessException(4002, "该停车场已存在区域编号 " + areaCode);
        }
        return parkingAreaMapper.selectById(area.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ParkingArea updateInfo(Long id, ParkingAreaSaveRequest request) {
        ParkingArea existing = requireArea(id);

        String areaName = TextNormalizer.normalizeText(request.getAreaName());
        if (areaName.isEmpty()) {
            throw new BusinessException(4001, "区域名称不能为空");
        }
        if (areaName.length() > NAME_MAX_LENGTH) {
            throw new BusinessException(4001, "区域名称不能超过 " + NAME_MAX_LENGTH + " 个字符");
        }

        ParkingArea updating = new ParkingArea();
        updating.setId(existing.getId());
        updating.setAreaName(areaName);
        updating.setDescription(normalizeDescription(request.getDescription()));
        parkingAreaMapper.updateInfo(updating);
        return parkingAreaMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public ParkingArea changeStatus(Long id, String targetStatus) {
        String status = parseStatus(targetStatus);
        ParkingArea existing = requireArea(id);

        if (status.equals(existing.getStatus())) {
            // 幂等：重复点「停用」不再报错，直接返回当前状态
            return existing;
        }

        if ("DISABLED".equals(status)
                && parkingAreaMapper.countParkingRecordsByArea(id) > 0) {
            throw new BusinessException(4007, "该区域还有在场车辆，不能停用");
        }

        parkingAreaMapper.updateStatus(id, status);
        return parkingAreaMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireArea(id);

        int spaceCount = parkingAreaMapper.countSpacesByArea(id);
        if (spaceCount > 0) {
            throw new BusinessException(4008, "该区域下还有 " + spaceCount + " 个车位，不能删除");
        }

        try {
            parkingAreaMapper.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            // 外键 fk_parking_space_area 是第二道保险
            throw new BusinessException(4008, "该区域仍被引用，不能删除");
        }
    }

    private ParkingArea requireArea(Long id) {
        if (id == null) {
            throw new BusinessException(4001, "区域编号不能为空");
        }
        ParkingArea area = parkingAreaMapper.selectById(id);
        if (area == null) {
            throw new BusinessException(4006, "区域不存在");
        }
        return area;
    }

    private String parseStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!"ACTIVE".equals(normalized) && !"DISABLED".equals(normalized)) {
            throw new BusinessException(4001, "状态只能是 ACTIVE 或 DISABLED");
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > DESCRIPTION_MAX_LENGTH) {
            throw new BusinessException(4001, "说明不能超过 " + DESCRIPTION_MAX_LENGTH + " 个字符");
        }
        return normalized;
    }
}
