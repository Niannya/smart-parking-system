package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCode;
import org.example.backend.dto.ParkingAreaCreateRequest;
import org.example.backend.dto.ParkingAreaStatusRequest;
import org.example.backend.dto.ParkingAreaUpdateRequest;
import org.example.backend.entity.ParkingArea;
import org.example.backend.mapper.ParkingAreaMapper;
import org.example.backend.support.CurrentUserProvider;
import org.example.backend.support.CurrentUserView;
import org.example.backend.support.RequestValues;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 停车区域业务（M2-02）。
 *
 * <p>业务规则来源：接口契约第 3 节、任务书 2.1、数据字典 6.4：</p>
 * <ol>
 *   <li>按 lotId 查询并按 areaCode 排序；</li>
 *   <li>只有启用（ACTIVE）停车场能新增/启用区域；</li>
 *   <li>区域编号同场唯一，先查后插，数据库唯一约束作为并发兜底（4002）；</li>
 *   <li>更新只允许名称与说明，归属与编号不可改；</li>
 *   <li>停用前先锁停车场行，再检查本区域是否有 PARKING 记录（4004）；</li>
 *   <li>区域下有车位不允许删除（4003）。</li>
 * </ol>
 */
@Service
public class ParkingAreaService {

    private static final int CODE_MAX_LENGTH = 20;
    private static final int NAME_MAX_LENGTH = 50;
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    private static final String ACTIVE = "ACTIVE";
    private static final String DISABLED = "DISABLED";

    private final ParkingAreaMapper parkingAreaMapper;
    private final CurrentUserProvider currentUserProvider;

    public ParkingAreaService(ParkingAreaMapper parkingAreaMapper,
                              CurrentUserProvider currentUserProvider) {
        this.parkingAreaMapper = parkingAreaMapper;
        this.currentUserProvider = currentUserProvider;
    }

    public List<ParkingArea> list(Long lotId, String status) {
        requireAdmin();
        Long requiredLotId = RequestValues.requirePositive(lotId, "lotId 必填，且必须为正整数");
        String normalizedStatus = RequestValues.parseStatus(status, true, ACTIVE, DISABLED);
        return parkingAreaMapper.selectByLot(requiredLotId, normalizedStatus);
    }

    @Transactional(rollbackFor = Exception.class)
    public ParkingArea create(ParkingAreaCreateRequest request) {
        requireAdmin();
        request.rejectUnknownFields();

        Long lotId = RequestValues.requirePositive(request.getLotId(), "lotId 必填，且必须为正整数");
        String areaCode = RequestValues.requireCode(request.getAreaCode(), "区域编号", CODE_MAX_LENGTH);
        String areaName = RequestValues.requireText(request.getAreaName(), "区域名称", NAME_MAX_LENGTH);
        String description = RequestValues.normalizeDescription(request.getDescription(), DESCRIPTION_MAX_LENGTH);

        // 先锁停车场行，再判断存在/启用：停用停车场的区域维护不能与入场、车位维护穿插
        String lotStatus = parkingAreaMapper.lockLotStatus(lotId);
        if (lotStatus == null) {
            throw new BusinessException(ErrorCode.LOT_NOT_FOUND, "停车场不存在");
        }
        if (!ACTIVE.equals(lotStatus)) {
            throw new BusinessException(ErrorCode.LOT_DISABLED, "该停车场已停用，不能新增区域");
        }

        if (parkingAreaMapper.countByLotAndCode(lotId, areaCode) > 0) {
            throw new BusinessException(ErrorCode.AREA_CODE_EXISTS, "同一停车场内区域编号已存在");
        }

        ParkingArea area = new ParkingArea();
        area.setParkingLotId(lotId);
        area.setAreaCode(areaCode);
        area.setAreaName(areaName);
        area.setDescription(description);
        try {
            parkingAreaMapper.insert(area);
        } catch (DuplicateKeyException e) {
            // 并发下两个请求同时通过查重，由 uk_parking_area_lot_code 拦下第二个
            throw new BusinessException(ErrorCode.AREA_CODE_EXISTS, "同一停车场内区域编号已存在");
        }
        return parkingAreaMapper.selectById(area.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ParkingArea updateInfo(Long id, ParkingAreaUpdateRequest request) {
        requireAdmin();
        request.rejectUnknownFields();

        Long areaId = RequestValues.requirePositive(id, "区域 ID 必须为正整数");
        ParkingArea existing = requireArea(areaId);

        String areaName = RequestValues.requireText(request.getAreaName(), "区域名称", NAME_MAX_LENGTH);
        String description = RequestValues.normalizeDescription(request.getDescription(), DESCRIPTION_MAX_LENGTH);

        ParkingArea updating = new ParkingArea();
        updating.setId(existing.getId());
        updating.setAreaName(areaName);
        updating.setDescription(description);
        parkingAreaMapper.updateInfo(updating);
        return parkingAreaMapper.selectById(areaId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ParkingArea changeStatus(Long id, ParkingAreaStatusRequest request) {
        requireAdmin();
        request.rejectUnknownFields();

        Long areaId = RequestValues.requirePositive(id, "区域 ID 必须为正整数");
        String targetStatus = RequestValues.parseStatus(request.getStatus(), false, ACTIVE, DISABLED);
        ParkingArea existing = requireArea(areaId);

        // 区域启停同样先锁停车场，保持「停车场 → 区域 → 车位」的加锁顺序
        String lotStatus = parkingAreaMapper.lockLotStatusByAreaId(areaId);
        if (lotStatus == null) {
            throw new BusinessException(ErrorCode.LOT_NOT_FOUND, "停车场不存在");
        }
        if (ACTIVE.equals(targetStatus) && !ACTIVE.equals(lotStatus)) {
            throw new BusinessException(ErrorCode.LOT_DISABLED, "该停车场已停用，不能启用区域");
        }
        if (targetStatus.equals(existing.getStatus())) {
            // 幂等：重复点同一个按钮直接返回当前状态
            return existing;
        }
        if (DISABLED.equals(targetStatus) && parkingAreaMapper.countParkingRecordsByArea(areaId) > 0) {
            throw new BusinessException(ErrorCode.AREA_HAS_ACTIVE_RECORD, "该区域还有在场车辆，不能停用");
        }

        int updated = parkingAreaMapper.updateStatus(areaId, targetStatus, existing.getStatus());
        if (updated != 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "区域状态已变化，请刷新后重试");
        }
        return parkingAreaMapper.selectById(areaId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireAdmin();
        Long areaId = RequestValues.requirePositive(id, "区域 ID 必须为正整数");
        ParkingArea existing = requireArea(areaId);

        // 与新增车位保持同一加锁顺序：先锁停车场，再检查车位数量，避免「删区域」与「加车位」互相穿插
        String lotStatus = parkingAreaMapper.lockLotStatus(existing.getParkingLotId());
        if (lotStatus == null) {
            throw new BusinessException(ErrorCode.LOT_NOT_FOUND, "停车场不存在");
        }

        int spaceCount = parkingAreaMapper.countSpacesByArea(areaId);
        if (spaceCount > 0) {
            throw new BusinessException(ErrorCode.AREA_HAS_SPACES, "该区域下还有 " + spaceCount + " 个车位，不能删除");
        }

        try {
            parkingAreaMapper.deleteById(areaId);
        } catch (DataIntegrityViolationException e) {
            // 外键 fk_parking_space_area / fk_parking_record_space 是第二道保险
            throw new BusinessException(ErrorCode.AREA_HAS_SPACES, "该区域仍被关联数据引用，不能删除");
        }
    }

    private ParkingArea requireArea(Long id) {
        ParkingArea area = parkingAreaMapper.selectById(id);
        if (area == null) {
            throw new BusinessException(ErrorCode.AREA_NOT_FOUND, "停车区域不存在");
        }
        return area;
    }

    private CurrentUserView requireAdmin() {
        CurrentUserView user = currentUserProvider.currentUser();
        if (!user.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号无权执行该操作");
        }
        return user;
    }
}
