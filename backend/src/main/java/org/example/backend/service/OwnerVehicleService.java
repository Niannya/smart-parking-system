package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCode;
import org.example.backend.dto.OwnerVehicleCreateRequest;
import org.example.backend.dto.VehicleStatusRequest;
import org.example.backend.entity.Vehicle;
import org.example.backend.mapper.VehicleMapper;
import org.example.backend.support.CurrentUserProvider;
import org.example.backend.support.CurrentUserView;
import org.example.backend.support.PlateNormalizer;
import org.example.backend.support.RequestValues;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 车主本人车辆业务（M2-04）。
 *
 * <p>硬性要求：</p>
 * <ol>
 *   <li>{@code ownerUserId} 只能来自当前会话，绝不接受请求参数；</li>
 *   <li>车牌统一「去空白 + 转大写」后再校验格式与唯一性（4204 / 4202）；</li>
 *   <li>列表只读本人车辆；启停 SQL 带 {@code owner_user_id} 条件；</li>
 *   <li>不存在返回 4201，存在但不是本人返回 4203，不返回他人车辆数据；</li>
 *   <li>只做启用/停用，不物理删除、不换绑；停用不影响既有停车记录。</li>
 * </ol>
 */
@Service
public class OwnerVehicleService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DISABLED = "DISABLED";

    private final VehicleMapper vehicleMapper;
    private final CurrentUserProvider currentUserProvider;

    public OwnerVehicleService(VehicleMapper vehicleMapper,
                               CurrentUserProvider currentUserProvider) {
        this.vehicleMapper = vehicleMapper;
        this.currentUserProvider = currentUserProvider;
    }

    public List<Vehicle> list() {
        CurrentUserView user = requireOwner();
        return vehicleMapper.selectByOwner(user.id());
    }

    @Transactional(rollbackFor = Exception.class)
    public Vehicle create(OwnerVehicleCreateRequest request) {
        CurrentUserView user = requireOwner();
        request.rejectUnknownFields();

        String plate = PlateNormalizer.normalize(request.getLicensePlate());
        if (plate.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_LICENSE_PLATE, "车牌不能为空");
        }
        if (!PlateNormalizer.isValid(plate)) {
            throw new BusinessException(ErrorCode.INVALID_LICENSE_PLATE, "车牌格式不正确，示例：京A12345");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setOwnerUserId(user.id());
        vehicle.setLicensePlate(plate);
        try {
            vehicleMapper.insert(vehicle);
        } catch (DuplicateKeyException e) {
            // uk_vehicle_license_plate 是全表唯一：已停用的车牌也不能被第二个账号绑定
            throw new BusinessException(ErrorCode.VEHICLE_PLATE_EXISTS, "该车牌已被绑定");
        }
        return vehicleMapper.selectById(vehicle.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public Vehicle changeStatus(Long id, VehicleStatusRequest request) {
        CurrentUserView user = requireOwner();
        request.rejectUnknownFields();

        Long vehicleId = RequestValues.requirePositive(id, "车辆 ID 必须为正整数");
        String targetStatus = RequestValues.parseStatus(request.getStatus(), false, ACTIVE, DISABLED);

        Vehicle existing = vehicleMapper.selectById(vehicleId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_FOUND, "车辆不存在");
        }
        if (!Objects.equals(existing.getOwnerUserId(), user.id())) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_OWNED, "不是本人车辆");
        }
        if (targetStatus.equals(existing.getStatus())) {
            return existing;
        }

        int updated = vehicleMapper.updateStatus(vehicleId, user.id(), targetStatus);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_OWNED, "不是本人车辆");
        }
        return vehicleMapper.selectById(vehicleId);
    }

    private CurrentUserView requireOwner() {
        CurrentUserView user = currentUserProvider.currentUser();
        if (!user.isOwner()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号无权执行该操作");
        }
        return user;
    }
}
