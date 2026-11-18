package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.BusinessException;
import org.example.backend.common.CurrentUser;
import org.example.backend.common.ErrorCodes;
import org.example.backend.common.Plates;
import org.example.backend.dto.vehicle.Vehicle;
import org.example.backend.dto.vehicle.VehicleBindRequest;
import org.example.backend.mapper.VehicleMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 车主车辆服务（成员 2 scaffold：最小可用实现，正式实现由成员 2 负责）。
 * owner_user_id 从 CurrentUser 取，不接受请求传 owner ID；只做启用/停用，无删除/换绑。
 */
@Service
@RequiredArgsConstructor
public class OwnerVehicleService {

    private final VehicleMapper vehicleMapper;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<Vehicle> listOwn() {
        return vehicleMapper.selectByOwner(currentUser.id());
    }

    @Transactional(rollbackFor = Exception.class)
    public Vehicle bind(VehicleBindRequest request) {
        Long ownerUserId = currentUser.id();
        String plate = Plates.normalize(request.getLicensePlate());
        Vehicle vehicle = new Vehicle();
        vehicle.setOwnerUserId(ownerUserId);
        vehicle.setLicensePlate(plate);
        vehicle.setStatus("ACTIVE");
        try {
            vehicleMapper.insert(vehicle);
        } catch (DuplicateKeyException e) {
            throw BusinessException.conflict(ErrorCodes.VEHICLE_PLATE_DUPLICATE, "车牌已绑定");
        }
        return vehicle;
    }

    @Transactional(rollbackFor = Exception.class)
    public Vehicle updateStatus(Long id, String status) {
        Long ownerUserId = currentUser.id();
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw BusinessException.badRequest(ErrorCodes.BAD_REQUEST, "状态仅支持 ACTIVE/DISABLED");
        }
        Vehicle own = vehicleMapper.selectByIdAndOwner(id, ownerUserId);
        if (own == null) {
            if (vehicleMapper.selectById(id) != null) {
                throw BusinessException.forbidden(ErrorCodes.VEHICLE_NOT_OWNED, "不是本人车辆");
            }
            throw BusinessException.notFound(ErrorCodes.VEHICLE_NOT_FOUND, "车辆不存在");
        }
        vehicleMapper.updateStatus(id, ownerUserId, status);
        own.setStatus(status);
        return own;
    }
}
