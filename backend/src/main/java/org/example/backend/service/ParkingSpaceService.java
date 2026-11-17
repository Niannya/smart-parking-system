package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.dto.ParkingSpaceCreateRequest;
import org.example.backend.dto.ParkingSpaceStatusRequest;
import org.example.backend.dto.ParkingSpaceUpdateRequest;
import org.example.backend.entity.ParkingArea;
import org.example.backend.entity.ParkingSpace;
import org.example.backend.mapper.ParkingSpaceMapper;
import org.example.backend.support.CurrentUserProvider;
import org.example.backend.support.CurrentUserView;
import org.example.backend.support.RequestValues;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 停车位业务（M2-03）。
 *
 * <p>核心约束：</p>
 * <ol>
 *   <li>归属由 {@code parking_area → parking_lot} JOIN 反查，不接受请求体传 lotId；</li>
 *   <li>新增只在启用停车场 + 启用区域；编号仅在同一个区域唯一（4102）；</li>
 *   <li>PUT 只允许 FREE；改编号时若有停车历史返回 4104；</li>
 *   <li>状态 API 只接受 FREE/DISABLED，OCCUPIED 一律拒绝（4103），外部不能设置 OCCUPIED；</li>
 *   <li>有历史记录的车位不能物理删除（4104）。</li>
 * </ol>
 */
@Service
public class ParkingSpaceService {

    private static final int CODE_MAX_LENGTH = 20;
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    private static final String FREE = "FREE";
    private static final String OCCUPIED = "OCCUPIED";
    private static final String DISABLED = "DISABLED";
    private static final String ACTIVE = "ACTIVE";

    private final ParkingSpaceMapper parkingSpaceMapper;
    private final CurrentUserProvider currentUserProvider;

    public ParkingSpaceService(ParkingSpaceMapper parkingSpaceMapper,
                               CurrentUserProvider currentUserProvider) {
        this.parkingSpaceMapper = parkingSpaceMapper;
        this.currentUserProvider = currentUserProvider;
    }

    public List<ParkingSpace> list(Long lotId, Long areaId, String status) {
        requireAdmin();
        Long requiredLotId = RequestValues.requirePositive(lotId, "lotId 必填，且必须为正整数");
        String normalizedStatus = RequestValues.parseStatus(status, true, FREE, OCCUPIED, DISABLED);

        if (areaId != null) {
            Long requiredAreaId = RequestValues.requirePositive(areaId, "areaId 必须为正整数");
            ParkingArea area = parkingSpaceMapper.selectAreaById(requiredAreaId);
            if (area == null) {
                throw new BusinessException(4001, "停车区域不存在");
            }
            if (!Objects.equals(area.getParkingLotId(), requiredLotId)) {
                throw new BusinessException(4000, "areaId 与 lotId 不属于同一个停车场");
            }
        }
        return parkingSpaceMapper.selectByLot(requiredLotId, areaId, normalizedStatus);
    }

    @Transactional(rollbackFor = Exception.class)
    public ParkingSpace create(ParkingSpaceCreateRequest request) {
        requireAdmin();
        request.rejectUnknownFields();

        Long areaId = RequestValues.requirePositive(request.getAreaId(), "areaId 必填，且必须为正整数");
        String spaceCode = RequestValues.requireCode(request.getSpaceCode(), "车位编号", CODE_MAX_LENGTH);
        String description = RequestValues.normalizeDescription(request.getDescription(), DESCRIPTION_MAX_LENGTH);

        ParkingArea area = parkingSpaceMapper.selectAreaById(areaId);
        if (area == null) {
            throw new BusinessException(4001, "停车区域不存在");
        }

        // 先锁停车场，再读区域状态：与区域启停、入场保持同一加锁顺序
        String lotStatus = parkingSpaceMapper.lockLotStatus(area.getParkingLotId());
        if (lotStatus == null) {
            throw new BusinessException(2001, "停车场不存在");
        }
        if (!ACTIVE.equals(lotStatus)) {
            throw new BusinessException(2005, "该停车场已停用，不能新增车位");
        }

        ParkingArea currentArea = parkingSpaceMapper.selectAreaById(areaId);
        if (currentArea == null) {
            throw new BusinessException(4001, "停车区域不存在");
        }
        if (!ACTIVE.equals(currentArea.getStatus())) {
            throw new BusinessException(4005, "该区域已停用，不能新增车位");
        }

        if (parkingSpaceMapper.countByAreaAndCode(areaId, spaceCode) > 0) {
            throw new BusinessException(4102, "同一区域内车位编号已存在");
        }

        ParkingSpace space = new ParkingSpace();
        space.setAreaId(areaId);
        space.setSpaceCode(spaceCode);
        space.setDescription(description);
        try {
            parkingSpaceMapper.insert(space);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(4102, "同一区域内车位编号已存在");
        }
        return parkingSpaceMapper.selectById(space.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ParkingSpace updateInfo(Long id, ParkingSpaceUpdateRequest request) {
        requireAdmin();
        request.rejectUnknownFields();

        Long spaceId = RequestValues.requirePositive(id, "车位 ID 必须为正整数");
        ParkingSpace existing = requireSpace(spaceId);

        String lotStatus = parkingSpaceMapper.lockLotStatusBySpaceId(spaceId);
        if (lotStatus == null) {
            throw new BusinessException(2001, "停车场不存在");
        }
        if (!FREE.equals(existing.getStatus())) {
            throw new BusinessException(4103, "只有空闲（FREE）车位可以修改");
        }

        String spaceCode = RequestValues.requireCode(request.getSpaceCode(), "车位编号", CODE_MAX_LENGTH);
        String description = RequestValues.normalizeDescription(request.getDescription(), DESCRIPTION_MAX_LENGTH);

        boolean codeChanged = !spaceCode.equals(existing.getSpaceCode());
        if (codeChanged && parkingSpaceMapper.countRecordsBySpace(spaceId) > 0) {
            throw new BusinessException(4104, "该车位已有停车历史，不能修改编号");
        }
        if (codeChanged && parkingSpaceMapper.countByAreaAndCode(existing.getAreaId(), spaceCode) > 0) {
            throw new BusinessException(4102, "同一区域内车位编号已存在");
        }

        int updated = parkingSpaceMapper.updateInfo(spaceId, spaceCode, description);
        if (updated != 1) {
            throw new BusinessException(4103, "车位状态已变化，请刷新后重试");
        }
        return parkingSpaceMapper.selectById(spaceId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ParkingSpace changeStatus(Long id, ParkingSpaceStatusRequest request) {
        requireAdmin();
        request.rejectUnknownFields();

        Long spaceId = RequestValues.requirePositive(id, "车位 ID 必须为正整数");
        String targetStatus = RequestValues.parseStatus(request.getStatus(), false, FREE, DISABLED);
        ParkingSpace existing = requireSpace(spaceId);

        String lotStatus = parkingSpaceMapper.lockLotStatusBySpaceId(spaceId);
        if (lotStatus == null) {
            throw new BusinessException(2001, "停车场不存在");
        }
        if (OCCUPIED.equals(existing.getStatus())) {
            throw new BusinessException(4103, "占用中的车位不能停用或启用");
        }
        if (targetStatus.equals(existing.getStatus())) {
            return existing;
        }

        if (DISABLED.equals(targetStatus)) {
            int updated = parkingSpaceMapper.updateStatus(spaceId, DISABLED, FREE);
            if (updated != 1) {
                throw new BusinessException(4103, "车位状态已变化，请刷新后重试");
            }
            return parkingSpaceMapper.selectById(spaceId);
        }

        // 启用：停车场与区域都必须处于启用状态
        if (!ACTIVE.equals(lotStatus)) {
            throw new BusinessException(2005, "该停车场已停用，不能启用车位");
        }
        ParkingArea area = parkingSpaceMapper.selectAreaById(existing.getAreaId());
        if (area == null) {
            throw new BusinessException(4001, "停车区域不存在");
        }
        if (!ACTIVE.equals(area.getStatus())) {
            throw new BusinessException(4005, "该区域已停用，不能启用该车位");
        }

        int updated = parkingSpaceMapper.updateStatus(spaceId, FREE, DISABLED);
        if (updated != 1) {
            throw new BusinessException(4103, "车位状态已变化，请刷新后重试");
        }
        return parkingSpaceMapper.selectById(spaceId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireAdmin();
        Long spaceId = RequestValues.requirePositive(id, "车位 ID 必须为正整数");
        ParkingSpace existing = requireSpace(spaceId);

        // 删除与入场、区域/车位启停保持同一加锁顺序：先锁所属停车场，避免删除与入场互相穿插
        String lotStatus = parkingSpaceMapper.lockLotStatusBySpaceId(spaceId);
        if (lotStatus == null) {
            throw new BusinessException(2001, "停车场不存在");
        }

        if (!FREE.equals(existing.getStatus())) {
            throw new BusinessException(4103, "只有空闲（FREE）车位可以删除");
        }
        if (parkingSpaceMapper.countRecordsBySpace(spaceId) > 0) {
            throw new BusinessException(4104, "该车位已有停车历史，不能删除");
        }

        try {
            int deleted = parkingSpaceMapper.deleteById(spaceId);
            if (deleted != 1) {
                throw new BusinessException(4103, "车位状态已变化，请刷新后重试");
            }
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(4104, "该车位仍被停车记录引用，不能删除");
        }
    }

    private ParkingSpace requireSpace(Long id) {
        ParkingSpace space = parkingSpaceMapper.selectById(id);
        if (space == null) {
            throw new BusinessException(4101, "车位不存在");
        }
        return space;
    }

    private CurrentUserView requireAdmin() {
        CurrentUserView user = currentUserProvider.currentUser();
        if (!user.isAdmin()) {
            throw new BusinessException(1004, "当前账号无权执行该操作");
        }
        return user;
    }
}
