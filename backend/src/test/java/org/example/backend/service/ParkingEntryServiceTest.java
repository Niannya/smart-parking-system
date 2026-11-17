package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.dto.ParkingEntryRequest;
import org.example.backend.dto.ParkingEntryResult;
import org.example.backend.dto.ParkingSpaceAllocation;
import org.example.backend.entity.ParkingRecord;
import org.example.backend.mapper.ParkingEntryMapper;
import org.example.backend.support.CurrentUserProvider;
import org.example.backend.support.CurrentUserView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingEntryServiceTest {

    private static final CurrentUserView ADMIN = new CurrentUserView(1L, CurrentUserView.ROLE_ADMIN);

    @Mock
    private ParkingEntryMapper parkingEntryMapper;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private ParkingEntryDependency parkingEntryDependency;
    private ParkingEntryService service;

    @BeforeEach
    void setUp() {
        service = new ParkingEntryService(parkingEntryMapper, currentUserProvider, parkingEntryDependency);

        lenient().when(currentUserProvider.currentUser()).thenReturn(ADMIN);
        lenient().when(parkingEntryDependency.findActiveFeeRuleId(1L)).thenReturn(3L);
        lenient().when(parkingEntryMapper.countActiveByPlate(anyString())).thenReturn(0);
        lenient().when(parkingEntryMapper.selectFreeSpaceForUpdate(1L)).thenReturn(space(11L, 2L, "A", "001"));
        lenient().when(parkingEntryMapper.occupySpace(11L)).thenReturn(1);
        lenient().when(parkingEntryMapper.selectActiveVehicleIdByPlate(anyString())).thenReturn(5L);
        lenient().when(parkingEntryMapper.insertRecord(any(ParkingRecord.class))).thenAnswer(invocation -> {
            ParkingRecord record = invocation.getArgument(0);
            record.setId(101L);
            return 1;
        });
    }

    @Test
    void entersSuccessfullyAndWritesAllRecordFields() {
        ParkingEntryResult result = service.enter(request("京A12345", 1L));

        assertEquals(101L, result.getId());
        assertEquals(1L, result.getLotId());
        assertEquals("京A12345", result.getLicensePlate());
        assertEquals(11L, result.getSpaceId());
        assertEquals("A", result.getAreaCode());
        assertEquals("001", result.getSpaceCode());
        assertEquals(3L, result.getFeeRuleId());
        assertEquals("PARKING", result.getStatus());
        assertNotNull(result.getEntryTime());
        assertEquals(32, result.getRecordNo().length());
        assertTrue(result.getRecordNo().startsWith("R"));

        ArgumentCaptor<ParkingRecord> captor = ArgumentCaptor.forClass(ParkingRecord.class);
        verify(parkingEntryMapper).insertRecord(captor.capture());
        ParkingRecord record = captor.getValue();
        assertEquals(1L, record.getParkingLotId());
        assertEquals(5L, record.getVehicleId());
        assertEquals("京A12345", record.getLicensePlate());
        assertEquals(11L, record.getParkingSpaceId());
        assertEquals(3L, record.getFeeRuleId());
        assertEquals(1L, record.getEntryAdminId());
        assertEquals("PARKING", record.getStatus());
    }

    @Test
    void rejectsUnknownRequestFieldsBeforeAnyDatabaseWork() {
        ParkingEntryRequest request = request("京A12345", 1L);
        request.captureUnknownField("feeRuleId", 3);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.enter(request));

        assertEquals(4000, exception.getCode());
        verifyNoInteractions(currentUserProvider, parkingEntryMapper, parkingEntryDependency);
    }

    @Test
    void rejectsBlankAndMalformedPlate() {
        assertEquals(4204, assertThrows(BusinessException.class,
                () -> service.enter(request("   ", 1L))).getCode());
        assertEquals(4204, assertThrows(BusinessException.class,
                () -> service.enter(request("A12345", 1L))).getCode());
        verifyNoInteractions(currentUserProvider, parkingEntryMapper, parkingEntryDependency);
    }

    @Test
    void rejectsNonAdmin() {
        when(currentUserProvider.currentUser()).thenReturn(new CurrentUserView(2L, CurrentUserView.ROLE_OWNER));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.enter(request("京A12345", 1L)));

        assertEquals(1004, exception.getCode());
        verify(parkingEntryDependency, never()).lockActiveLot(anyLong());
        verifyNoInteractions(parkingEntryMapper);
    }

    @Test
    void rejectsMissingLotIdAfterAdminCheck() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.enter(request("京A12345", null)));

        assertEquals(4000, exception.getCode());
        verify(parkingEntryDependency, never()).lockActiveLot(anyLong());
    }

    @Test
    void rejectsDuplicateActivePlate() {
        when(parkingEntryMapper.countActiveByPlate("京A12345")).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.enter(request("京A12345", 1L)));

        assertEquals(4301, exception.getCode());
        verify(parkingEntryDependency).lockActiveLot(1L);
        verify(parkingEntryDependency, never()).findActiveFeeRuleId(anyLong());
        verify(parkingEntryMapper, never()).selectFreeSpaceForUpdate(anyLong());
    }

    @Test
    void rejectsLotWithoutActiveFeeRule() {
        when(parkingEntryDependency.findActiveFeeRuleId(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.enter(request("京A12345", 1L)));

        assertEquals(4303, exception.getCode());
        verify(parkingEntryMapper, never()).selectFreeSpaceForUpdate(anyLong());
    }

    @Test
    void rejectsLotWithoutFreeSpace() {
        when(parkingEntryMapper.selectFreeSpaceForUpdate(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.enter(request("京A12345", 1L)));

        assertEquals(4302, exception.getCode());
        verify(parkingEntryMapper, never()).occupySpace(anyLong());
    }

    @Test
    void rejectsWhenConditionalOccupancyLosesRace() {
        when(parkingEntryMapper.occupySpace(11L)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.enter(request("京A12345", 1L)));

        assertEquals(4304, exception.getCode());
        verify(parkingEntryMapper, never()).insertRecord(any(ParkingRecord.class));
    }

    @Test
    void disabledOrUnboundVehicleIsTemporaryWithNullVehicleId() {
        when(parkingEntryMapper.selectActiveVehicleIdByPlate("京A12345")).thenReturn(null);

        service.enter(request("京A12345", 1L));

        ArgumentCaptor<ParkingRecord> captor = ArgumentCaptor.forClass(ParkingRecord.class);
        verify(parkingEntryMapper).insertRecord(captor.capture());
        assertNull(captor.getValue().getVehicleId());
        assertEquals("京A12345", captor.getValue().getLicensePlate());
    }

    @Test
    void retriesRecordNoWhenInsertReturnsZero() {
        when(parkingEntryMapper.insertRecord(any(ParkingRecord.class))).thenReturn(0, 1);

        service.enter(request("京A12345", 1L));

        ArgumentCaptor<ParkingRecord> captor = ArgumentCaptor.forClass(ParkingRecord.class);
        verify(parkingEntryMapper, times(2)).insertRecord(captor.capture());
        assertTrue(captor.getAllValues().get(0).getRecordNo().length() == 32);
        assertTrue(captor.getAllValues().get(1).getRecordNo().length() == 32);
        assertTrue(captor.getAllValues().get(0).getRecordNo().startsWith("R"));
        assertTrue(captor.getAllValues().get(1).getRecordNo().startsWith("R"));
    }

    @Test
    void translatesActivePlateUniqueConflictTo4301() {
        when(parkingEntryMapper.insertRecord(any(ParkingRecord.class)))
                .thenThrow(new DuplicateKeyException("duplicate key ... uk_parking_record_active_plate"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.enter(request("京A12345", 1L)));

        assertEquals(4301, exception.getCode());
    }

    @Test
    void translatesActiveSpaceUniqueConflictTo4304() {
        when(parkingEntryMapper.insertRecord(any(ParkingRecord.class)))
                .thenThrow(new DuplicateKeyException("duplicate key ... uk_parking_record_active_space"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.enter(request("京A12345", 1L)));

        assertEquals(4304, exception.getCode());
    }

    @Test
    void normalizesPlateBeforeCheckingActiveRecord() {
        service.enter(request("  京a12345  ", 1L));

        verify(parkingEntryMapper).countActiveByPlate("京A12345");
        ArgumentCaptor<ParkingRecord> captor = ArgumentCaptor.forClass(ParkingRecord.class);
        verify(parkingEntryMapper).insertRecord(captor.capture());
        assertEquals("京A12345", captor.getValue().getLicensePlate());
    }

    @Test
    void externalLockAndRuleLookupHappenInFixedOrder() {
        service.enter(request("京A12345", 1L));

        InOrder order = inOrder(parkingEntryDependency, parkingEntryMapper);
        order.verify(parkingEntryDependency).lockActiveLot(1L);
        order.verify(parkingEntryMapper).countActiveByPlate("京A12345");
        order.verify(parkingEntryDependency).findActiveFeeRuleId(1L);
        order.verify(parkingEntryMapper).selectFreeSpaceForUpdate(1L);
        order.verify(parkingEntryMapper).occupySpace(11L);
        order.verify(parkingEntryMapper).insertRecord(any(ParkingRecord.class));
    }

    @Test
    void recordNoIsAtMost32CharsAndStartsWithR() {
        String recordNo = ParkingEntryService.newRecordNo();
        assertEquals(32, recordNo.length());
        assertTrue(recordNo.startsWith("R"));
    }

    private ParkingEntryRequest request(String plate, Long lotId) {
        ParkingEntryRequest request = new ParkingEntryRequest();
        request.setLotId(lotId);
        request.setLicensePlate(plate);
        return request;
    }

    private ParkingSpaceAllocation space(Long id, Long areaId, String areaCode, String spaceCode) {
        ParkingSpaceAllocation space = new ParkingSpaceAllocation();
        space.setId(id);
        space.setAreaId(areaId);
        space.setAreaCode(areaCode);
        space.setSpaceCode(spaceCode);
        return space;
    }
}
