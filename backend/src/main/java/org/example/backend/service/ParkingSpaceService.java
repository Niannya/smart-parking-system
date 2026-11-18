package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCodes;
import org.example.backend.common.ParkingLotGuard;
import org.example.backend.dto.space.ParkingSpace;
import org.example.backend.dto.space.ParkingSpaceCreateRequest;
import org.example.backend.mapper.ParkingAreaMapper;
import org.example.backend.mapper.ParkingSpaceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 停车位服务（成员 2 scaffold：最小可用实现，正式实现由成员 2 负责）。
 */
@Service
@RequiredArgsConstructor
public class ParkingSpaceService {

    private final ParkingSpaceMapper spaceMapper;
    private final ParkingAreaMapper areaMapper;
    private final ParkingLotGuard parkingLotGuard;

    @Transactional(readOnly = true)
    public List<ParkingSpace> list(Long lotId, Long areaId, String status) {
        if (areaId != null && areaMapper.selectLotIdByArea(areaId) == null) {
            throw BusinessException.notFound(ErrorCodes.AREA_NOT_FOUND, "区域不存在");
        }
        return spaceMapper.selectByFilter(lotId, areaId, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public ParkingSpace create(ParkingSpaceCreateRequest request) {
        Long areaId = request.getAreaId();
        Long lotId = areaMapper.selectLotIdByArea(areaId);
        if (lotId == null) {
            throw BusinessException.notFound(ErrorCodes.AREA_NOT_FOUND, "区域不存在");
        }
        parkingLotGuard.lockActiveLot(lotId);
        String areaStatus = areaMapper.selectStatusById(areaId);
        if (!"ACTIVE".equals(areaStatus)) {
            throw BusinessException.conflict(ErrorCodes.AREA_NOT_FOUND, "区域已停用，不能新增车位");
        }
        ParkingSpace space = new ParkingSpace();
        space.setAreaId(areaId);
        space.setSpaceCode(request.getSpaceCode().trim().toUpperCase());
        space.setDescription(request.getDescription());
        spaceMapper.insert(space);
        return space;
    }
}
