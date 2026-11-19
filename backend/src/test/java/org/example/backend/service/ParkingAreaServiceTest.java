package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.dto.ParkingAreaCreateRequest;
import org.example.backend.dto.ParkingAreaStatusRequest;
import org.example.backend.dto.ParkingAreaUpdateRequest;
import org.example.backend.entity.ParkingArea;
import org.example.backend.mapper.ParkingAreaMapper;
import org.example.backend.support.CurrentUserProvider;
import org.example.backend.support.CurrentUserView;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingAreaServiceTest {

    private static final CurrentUserView ADMIN = new CurrentUserView(1L, "ADMIN");
    private static final CurrentUserView OWNER = new CurrentUserView(2L, "OWNER");

    @Mock
    private ParkingAreaMapper parkingAreaMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ParkingAreaService parkingAreaService;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser()).thenReturn(ADMIN);
    }

    @Test
    void ownerCannotListAreas() {
        when(currentUserProvider.currentUser()).thenReturn(OWNER);
        assertCode(() -> parkingAreaService.list(1L, null), 1004);
        verify(parkingAreaMapper, never()).selectByLot(any(), any());
    }

    @Test
    void listRequiresLotId() {
        assertCode(() -> parkingAreaService.list(null, null), 4000);
    }

    @Test
    void listRejectsUnknownStatus() {
        assertCode(() -> parkingAreaService.list(1L, "PARKING"), 4000);
    }

    @Test
    void createRejectsUnknownField() {
        ParkingAreaCreateRequest request = createRequest(1L);
        request.captureUnknownField("ownerUserId", 2L);
        assertCode(() -> parkingAreaService.create(request), 4000);
        verify(parkingAreaMapper, never()).insert(any());
    }

    @Test
    void createRejectsMissingParkingLot() {
        when(parkingAreaMapper.lockLotStatus(1L)).thenReturn(null);
        assertCode(() -> parkingAreaService.create(createRequest(1L)), 2001);
        verify(parkingAreaMapper, never()).insert(any());
    }

    @Test
    void createRejectsDisabledParkingLot() {
        when(parkingAreaMapper.lockLotStatus(1L)).thenReturn("DISABLED");
        assertCode(() -> parkingAreaService.create(createRequest(1L)), 2005);
        verify(parkingAreaMapper, never()).insert(any());
    }

    @Test
    void createRejectsDuplicateAreaCodeInSameLot() {
        when(parkingAreaMapper.lockLotStatus(1L)).thenReturn("ACTIVE");
        when(parkingAreaMapper.countByLotAndCode(1L, "A")).thenReturn(1);
        assertCode(() -> parkingAreaService.create(createRequest(1L)), 4002);
        verify(parkingAreaMapper, never()).insert(any());
    }

    @Test
    void createNormalizesAreaCodeAndReturnsSavedArea() {
        when(parkingAreaMapper.lockLotStatus(1L)).thenReturn("ACTIVE");
        when(parkingAreaMapper.countByLotAndCode(1L, "C")).thenReturn(0);
        doAnswer(invocation -> {
            ParkingArea area = invocation.getArgument(0);
            area.setId(9L);
            return 1;
        }).when(parkingAreaMapper).insert(any(ParkingArea.class));
        ParkingArea saved = area(9L, "C", "ACTIVE");
        when(parkingAreaMapper.selectById(9L)).thenReturn(saved);

        ParkingAreaCreateRequest request = createRequest(1L);
        request.setAreaCode(" c ");
        request.setAreaName(" C 区 ");

        ParkingArea result = parkingAreaService.create(request);

        assertThat(result.getId()).isEqualTo(9L);
        assertThat(result.getAreaCode()).isEqualTo("C");
    }

    @Test
    void updateCannotChangeAreaCode() {
        ParkingAreaUpdateRequest request = new ParkingAreaUpdateRequest();
        request.setAreaName("A 区");
        request.captureUnknownField("areaCode", "B");

        assertCode(() -> parkingAreaService.updateInfo(10L, request), 4000);
        verify(parkingAreaMapper, never()).updateInfo(any());
    }

    @Test
    void disableAreaWithParkingVehiclesIsRejected() {
        when(parkingAreaMapper.selectById(10L)).thenReturn(area(10L, "A", "ACTIVE"));
        when(parkingAreaMapper.lockLotStatusByAreaId(10L)).thenReturn("ACTIVE");
        when(parkingAreaMapper.countParkingRecordsByArea(10L)).thenReturn(2);

        ParkingAreaStatusRequest request = new ParkingAreaStatusRequest();
        request.setStatus("DISABLED");

        assertCode(() -> parkingAreaService.changeStatus(10L, request), 4004);
        verify(parkingAreaMapper, never()).updateStatus(any(), any(), any());
    }

    @Test
    void enableAreaOfDisabledLotIsRejected() {
        when(parkingAreaMapper.selectById(10L)).thenReturn(area(10L, "A", "DISABLED"));
        when(parkingAreaMapper.lockLotStatusByAreaId(10L)).thenReturn("DISABLED");

        ParkingAreaStatusRequest request = new ParkingAreaStatusRequest();
        request.setStatus("ACTIVE");

        assertCode(() -> parkingAreaService.changeStatus(10L, request), 2005);
    }

    @Test
    void deleteAreaWithSpacesIsRejected() {
        when(parkingAreaMapper.selectById(10L)).thenReturn(area(10L, "A", "ACTIVE"));
        when(parkingAreaMapper.lockLotStatus(1L)).thenReturn("ACTIVE");
        when(parkingAreaMapper.countSpacesByArea(10L)).thenReturn(3);

        assertCode(() -> parkingAreaService.delete(10L), 4003);
        verify(parkingAreaMapper, never()).deleteById(any());
    }

    @Test
    void deleteEmptyAreaSucceeds() {
        when(parkingAreaMapper.selectById(10L)).thenReturn(area(10L, "A", "DISABLED"));
        when(parkingAreaMapper.lockLotStatus(1L)).thenReturn("ACTIVE");
        when(parkingAreaMapper.countSpacesByArea(10L)).thenReturn(0);

        parkingAreaService.delete(10L);

        verify(parkingAreaMapper).deleteById(10L);
    }

    @Test
    void deleteMissingAreaIsRejected() {
        when(parkingAreaMapper.selectById(10L)).thenReturn(null);
        assertCode(() -> parkingAreaService.delete(10L), 4001);
    }

    @Test
    void listReturnsAreasOfLot() {
        ParkingArea first = area(1L, "A", "ACTIVE");
        when(parkingAreaMapper.selectByLot(1L, "ACTIVE")).thenReturn(List.of(first));

        assertThat(parkingAreaService.list(1L, "active")).containsExactly(first);
    }

    private ParkingAreaCreateRequest createRequest(Long lotId) {
        ParkingAreaCreateRequest request = new ParkingAreaCreateRequest();
        request.setLotId(lotId);
        request.setAreaCode("A");
        request.setAreaName("A 区");
        return request;
    }

    private ParkingArea area(Long id, String code, String status) {
        ParkingArea area = new ParkingArea();
        area.setId(id);
        area.setParkingLotId(1L);
        area.setAreaCode(code);
        area.setAreaName(code + " 区");
        area.setStatus(status);
        return area;
    }

    private void assertCode(ThrowingCallable callable, int expectedCode) {
        BusinessException exception = catchThrowableOfType(callable, BusinessException.class);
        assertThat(exception).isNotNull();
        assertThat(exception.getCode()).isEqualTo(expectedCode);
    }
}
