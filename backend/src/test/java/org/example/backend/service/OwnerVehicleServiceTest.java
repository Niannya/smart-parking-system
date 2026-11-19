package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.dto.OwnerVehicleCreateRequest;
import org.example.backend.dto.VehicleStatusRequest;
import org.example.backend.entity.Vehicle;
import org.example.backend.mapper.VehicleMapper;
import org.example.backend.support.CurrentUserProvider;
import org.example.backend.support.CurrentUserView;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerVehicleServiceTest {

    private static final CurrentUserView OWNER = new CurrentUserView(7L, "OWNER");
    private static final CurrentUserView OTHER_OWNER = new CurrentUserView(8L, "OWNER");

    @Mock
    private VehicleMapper vehicleMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private OwnerVehicleService ownerVehicleService;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser()).thenReturn(OWNER);
    }

    @Test
    void listOnlyQueriesCurrentOwner() {
        Vehicle vehicle = vehicle(1L, "京A12345", "ACTIVE", 7L);
        when(vehicleMapper.selectByOwner(7L)).thenReturn(List.of(vehicle));

        assertThat(ownerVehicleService.list()).containsExactly(vehicle);
    }

    @Test
    void adminCannotUseOwnerVehicleApi() {
        when(currentUserProvider.currentUser()).thenReturn(new CurrentUserView(1L, "ADMIN"));
        assertCode(ownerVehicleService::list, 1004);
        verify(vehicleMapper, never()).selectByOwner(any());
    }

    @Test
    void createRejectsOwnerUserIdInBody() {
        OwnerVehicleCreateRequest request = new OwnerVehicleCreateRequest();
        request.setLicensePlate("京A12345");
        request.captureUnknownField("ownerUserId", 8L);

        assertCode(() -> ownerVehicleService.create(request), 4000);
        verify(vehicleMapper, never()).insert(any());
    }

    @Test
    void createRejectsInvalidPlate() {
        OwnerVehicleCreateRequest request = new OwnerVehicleCreateRequest();
        request.setLicensePlate("京A1234");

        assertCode(() -> ownerVehicleService.create(request), 4204);
        verify(vehicleMapper, never()).insert(any());
    }

    @Test
    void createRejectsDuplicatePlate() {
        OwnerVehicleCreateRequest request = new OwnerVehicleCreateRequest();
        request.setLicensePlate(" 京 a 12345 ");
        when(vehicleMapper.insert(any(Vehicle.class))
        ).thenThrow(new DuplicateKeyException("duplicate key value violates unique constraint \"uk_vehicle_license_plate\""));

        assertCode(() -> ownerVehicleService.create(request), 4202);
    }

    @Test
    void createUsesCurrentUserAndNormalizedPlate() {
        OwnerVehicleCreateRequest request = new OwnerVehicleCreateRequest();
        request.setLicensePlate(" 京 a 12345 ");
        doAnswer(invocation -> {
            Vehicle vehicle = invocation.getArgument(0);
            vehicle.setId(5L);
            assertThat(vehicle.getOwnerUserId()).isEqualTo(7L);
            assertThat(vehicle.getLicensePlate()).isEqualTo("京A12345");
            return 1;
        }).when(vehicleMapper).insert(any(Vehicle.class));
        when(vehicleMapper.selectById(5L)).thenReturn(vehicle(5L, "京A12345", "ACTIVE", 7L));

        assertThat(ownerVehicleService.create(request).getLicensePlate()).isEqualTo("京A12345");
    }

    @Test
    void changeStatusRejectsMissingVehicle() {
        when(vehicleMapper.selectById(5L)).thenReturn(null);
        assertCode(() -> ownerVehicleService.changeStatus(5L, statusRequest("DISABLED")), 4201);
    }

    @Test
    void changeStatusRejectsAnotherOwnersVehicle() {
        when(vehicleMapper.selectById(5L)).thenReturn(vehicle(5L, "京A12345", "ACTIVE", 8L));
        assertCode(() -> ownerVehicleService.changeStatus(5L, statusRequest("DISABLED")), 4203);
        verify(vehicleMapper, never()).updateStatus(any(), any(), any());
    }

    @Test
    void changeStatusUpdatesOwnVehicle() {
        when(vehicleMapper.selectById(5L))
                .thenReturn(vehicle(5L, "京A12345", "ACTIVE", 7L))
                .thenReturn(vehicle(5L, "京A12345", "DISABLED", 7L));
        when(vehicleMapper.updateStatus(5L, 7L, "DISABLED")).thenReturn(1);

        assertThat(ownerVehicleService.changeStatus(5L, statusRequest("disabled")).getStatus())
                .isEqualTo("DISABLED");
    }

    @Test
    void changeStatusRejectsUnknownStatus() {
        assertCode(() -> ownerVehicleService.changeStatus(5L, statusRequest("PARKING")), 4000);
    }

    private VehicleStatusRequest statusRequest(String status) {
        VehicleStatusRequest request = new VehicleStatusRequest();
        request.setStatus(status);
        return request;
    }

    private Vehicle vehicle(Long id, String plate, String status, Long ownerUserId) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setLicensePlate(plate);
        vehicle.setStatus(status);
        vehicle.setOwnerUserId(ownerUserId);
        return vehicle;
    }

    private void assertCode(ThrowingCallable callable, int expectedCode) {
        BusinessException exception = catchThrowableOfType(callable, BusinessException.class);
        assertThat(exception).isNotNull();
        assertThat(exception.getCode()).isEqualTo(expectedCode);
    }
}
