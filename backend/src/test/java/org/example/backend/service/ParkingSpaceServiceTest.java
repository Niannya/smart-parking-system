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
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingSpaceServiceTest {

    private static final CurrentUserView ADMIN = new CurrentUserView(1L, "ADMIN");

    @Mock
    private ParkingSpaceMapper parkingSpaceMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ParkingSpaceService parkingSpaceService;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser()).thenReturn(ADMIN);
    }

    @Test
    void listRejectsAreaFromAnotherLot() {
        when(parkingSpaceMapper.selectAreaById(2L)).thenReturn(area(2L, 2L, "A", "ACTIVE"));
        assertCode(() -> parkingSpaceService.list(1L, 2L, null), 4000);
        verify(parkingSpaceMapper, never()).selectByLot(any(), any(), any());
    }

    @Test
    void listRejectsBlankStatusFilter() {
        // 契约：字段省略表示不过滤，空字符串过滤值是非法参数
        assertCode(() -> parkingSpaceService.list(1L, null, " "), 4000);
        verify(parkingSpaceMapper, never()).selectByLot(any(), any(), any());
    }

    @Test
    void createRejectsDisabledArea() {
        when(parkingSpaceMapper.selectAreaById(2L))
                .thenReturn(area(2L, 1L, "A", "ACTIVE"))
                .thenReturn(area(2L, 1L, "A", "DISABLED"));
        when(parkingSpaceMapper.lockLotStatus(1L)).thenReturn("ACTIVE");

        assertCode(() -> parkingSpaceService.create(createRequest(2L)), 4005);
        verify(parkingSpaceMapper, never()).insert(any());
    }

    @Test
    void createRejectsDisabledLot() {
        when(parkingSpaceMapper.selectAreaById(2L)).thenReturn(area(2L, 1L, "A", "ACTIVE"));
        when(parkingSpaceMapper.lockLotStatus(1L)).thenReturn("DISABLED");

        assertCode(() -> parkingSpaceService.create(createRequest(2L)), 2005);
    }

    @Test
    void createRejectsDuplicateCodeInSameArea() {
        when(parkingSpaceMapper.selectAreaById(2L)).thenReturn(area(2L, 1L, "A", "ACTIVE"));
        when(parkingSpaceMapper.lockLotStatus(1L)).thenReturn("ACTIVE");
        when(parkingSpaceMapper.countByAreaAndCode(2L, "011")).thenReturn(1);

        assertCode(() -> parkingSpaceService.create(createRequest(2L)), 4102);
        verify(parkingSpaceMapper, never()).insert(any());
    }

    @Test
    void createNormalizesSpaceCodeAndReturnsSavedSpace() {
        when(parkingSpaceMapper.selectAreaById(2L)).thenReturn(area(2L, 1L, "A", "ACTIVE"));
        when(parkingSpaceMapper.lockLotStatus(1L)).thenReturn("ACTIVE");
        when(parkingSpaceMapper.countByAreaAndCode(2L, "011")).thenReturn(0);
        doAnswer(invocation -> {
            ParkingSpace space = invocation.getArgument(0);
            space.setId(88L);
            return 1;
        }).when(parkingSpaceMapper).insert(any(ParkingSpace.class));
        ParkingSpace saved = space(88L, 2L, "011", "FREE");
        when(parkingSpaceMapper.selectById(88L)).thenReturn(saved);

        ParkingSpaceCreateRequest request = createRequest(2L);
        request.setSpaceCode(" 011 ");

        assertThat(parkingSpaceService.create(request).getId()).isEqualTo(88L);
    }

    @Test
    void updateOccupiedSpaceIsRejected() {
        when(parkingSpaceMapper.selectById(7L)).thenReturn(space(7L, 2L, "001", "OCCUPIED"));
        when(parkingSpaceMapper.lockLotStatusBySpaceId(7L)).thenReturn("ACTIVE");

        ParkingSpaceUpdateRequest request = new ParkingSpaceUpdateRequest();
        request.setSpaceCode("002");

        assertCode(() -> parkingSpaceService.updateInfo(7L, request), 4103);
        verify(parkingSpaceMapper, never()).updateInfo(any(), any(), any());
    }

    @Test
    void updateCodeWithParkingHistoryIsRejected() {
        when(parkingSpaceMapper.selectById(7L)).thenReturn(space(7L, 2L, "001", "FREE"));
        when(parkingSpaceMapper.lockLotStatusBySpaceId(7L)).thenReturn("ACTIVE");
        when(parkingSpaceMapper.countRecordsBySpace(7L)).thenReturn(4);

        ParkingSpaceUpdateRequest request = new ParkingSpaceUpdateRequest();
        request.setSpaceCode("002");

        assertCode(() -> parkingSpaceService.updateInfo(7L, request), 4104);
    }

    @Test
    void statusApiRejectsOccupiedAsTarget() {
        ParkingSpaceStatusRequest request = new ParkingSpaceStatusRequest();
        request.setStatus("OCCUPIED");
        assertCode(() -> parkingSpaceService.changeStatus(7L, request), 4000);
    }

    @Test
    void enableSpaceInDisabledAreaIsRejected() {
        when(parkingSpaceMapper.selectById(7L)).thenReturn(space(7L, 2L, "001", "DISABLED"));
        when(parkingSpaceMapper.lockLotStatusBySpaceId(7L)).thenReturn("ACTIVE");
        when(parkingSpaceMapper.selectAreaById(2L)).thenReturn(area(2L, 1L, "A", "DISABLED"));

        ParkingSpaceStatusRequest request = new ParkingSpaceStatusRequest();
        request.setStatus("FREE");

        assertCode(() -> parkingSpaceService.changeStatus(7L, request), 4005);
        verify(parkingSpaceMapper, never()).updateStatus(any(), any(), any());
    }

    @Test
    void disableOccupiedSpaceIsRejected() {
        when(parkingSpaceMapper.selectById(7L)).thenReturn(space(7L, 2L, "001", "OCCUPIED"));
        when(parkingSpaceMapper.lockLotStatusBySpaceId(7L)).thenReturn("ACTIVE");

        ParkingSpaceStatusRequest request = new ParkingSpaceStatusRequest();
        request.setStatus("DISABLED");

        assertCode(() -> parkingSpaceService.changeStatus(7L, request), 4103);
    }

    @Test
    void freeDisabledFreeStateMachineSucceeds() {
        ParkingSpace free = space(7L, 2L, "001", "FREE");
        ParkingSpace disabled = space(7L, 2L, "001", "DISABLED");
        // changeStatus 每次先 requireSpace，再在更新后 selectById 回读
        when(parkingSpaceMapper.selectById(7L)).thenReturn(free, disabled, disabled, free);
        when(parkingSpaceMapper.lockLotStatusBySpaceId(7L)).thenReturn("ACTIVE", "ACTIVE");
        when(parkingSpaceMapper.updateStatus(7L, "DISABLED", "FREE")).thenReturn(1);
        when(parkingSpaceMapper.updateStatus(7L, "FREE", "DISABLED")).thenReturn(1);
        when(parkingSpaceMapper.selectAreaById(2L)).thenReturn(area(2L, 1L, "A", "ACTIVE"));

        ParkingSpaceStatusRequest disable = new ParkingSpaceStatusRequest();
        disable.setStatus("DISABLED");
        assertThat(parkingSpaceService.changeStatus(7L, disable).getStatus()).isEqualTo("DISABLED");

        ParkingSpaceStatusRequest enable = new ParkingSpaceStatusRequest();
        enable.setStatus("FREE");
        assertThat(parkingSpaceService.changeStatus(7L, enable).getStatus()).isEqualTo("FREE");

        verify(parkingSpaceMapper).updateStatus(7L, "DISABLED", "FREE");
        verify(parkingSpaceMapper).updateStatus(7L, "FREE", "DISABLED");
    }

    @Test
    void deleteSpaceWithHistoryIsRejected() {
        when(parkingSpaceMapper.selectById(7L)).thenReturn(space(7L, 2L, "001", "FREE"));
        when(parkingSpaceMapper.lockLotStatusBySpaceId(7L)).thenReturn("ACTIVE");
        when(parkingSpaceMapper.countRecordsBySpace(7L)).thenReturn(1);

        assertCode(() -> parkingSpaceService.delete(7L), 4104);
        verify(parkingSpaceMapper, never()).deleteById(any());
    }

    @Test
    void deleteNonFreeSpaceIsRejected() {
        when(parkingSpaceMapper.selectById(7L)).thenReturn(space(7L, 2L, "001", "DISABLED"));
        when(parkingSpaceMapper.lockLotStatusBySpaceId(7L)).thenReturn("ACTIVE");
        assertCode(() -> parkingSpaceService.delete(7L), 4103);
    }

    @Test
    void deleteFreeSpaceWithoutHistorySucceeds() {
        when(parkingSpaceMapper.selectById(7L)).thenReturn(space(7L, 2L, "001", "FREE"));
        when(parkingSpaceMapper.lockLotStatusBySpaceId(7L)).thenReturn("ACTIVE");
        when(parkingSpaceMapper.countRecordsBySpace(7L)).thenReturn(0);
        when(parkingSpaceMapper.deleteById(7L)).thenReturn(1);

        parkingSpaceService.delete(7L);

        verify(parkingSpaceMapper).deleteById(7L);
    }

    private ParkingSpaceCreateRequest createRequest(Long areaId) {
        ParkingSpaceCreateRequest request = new ParkingSpaceCreateRequest();
        request.setAreaId(areaId);
        request.setSpaceCode("011");
        return request;
    }

    private ParkingArea area(Long id, Long lotId, String code, String status) {
        ParkingArea area = new ParkingArea();
        area.setId(id);
        area.setParkingLotId(lotId);
        area.setAreaCode(code);
        area.setAreaName(code + " 区");
        area.setStatus(status);
        return area;
    }

    private ParkingSpace space(Long id, Long areaId, String code, String status) {
        ParkingSpace space = new ParkingSpace();
        space.setId(id);
        space.setAreaId(areaId);
        space.setAreaCode("A");
        space.setSpaceCode(code);
        space.setStatus(status);
        return space;
    }

    private void assertCode(ThrowingCallable callable, int expectedCode) {
        BusinessException exception = catchThrowableOfType(callable, BusinessException.class);
        assertThat(exception).isNotNull();
        assertThat(exception.getCode()).isEqualTo(expectedCode);
    }
}
