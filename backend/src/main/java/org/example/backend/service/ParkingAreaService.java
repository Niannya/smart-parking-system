package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCodes;
import org.example.backend.common.ParkingLotGuard;
import org.example.backend.dto.area.ParkingArea;
import org.example.backend.dto.area.ParkingAreaCreateRequest;
import org.example.backend.mapper.ParkingAreaMapper;
import org.example.backend.mapper.ParkingLotMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 停车区域服务（成员 2 scaffold：最小可用实现，正式实现由成员 2 负责）。
 */
@Service
@RequiredArgsConstructor
public class ParkingAreaService {

    private final ParkingAreaMapper areaMapper;
    private final ParkingLotMapper lotMapper;
    private final ParkingLotGuard parkingLotGuard;

    @Transactional(readOnly = true)
    public List<ParkingArea> listByLot(Long lotId) {
        if (lotMapper.selectStatusById(lotId) == null) {
            throw BusinessException.notFound(ErrorCodes.LOT_NOT_FOUND, "停车场不存在");
        }
        return areaMapper.selectByLot(lotId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ParkingArea create(ParkingAreaCreateRequest request) {
        Long lotId = request.getLotId();
        parkingLotGuard.lockActiveLot(lotId);
        String areaCode = request.getAreaCode().trim().toUpperCase();
        if (areaMapper.countByLotAndCode(lotId, areaCode) > 0) {
            throw BusinessException.conflict(ErrorCodes.AREA_CODE_DUPLICATE, "同一停车场内区域编号已存在");
        }
        ParkingArea area = new ParkingArea();
        area.setParkingLotId(lotId);
        area.setAreaCode(areaCode);
        area.setAreaName(request.getAreaName().trim());
        area.setDescription(request.getDescription());
        areaMapper.insert(area);
        return area;
    }
}
