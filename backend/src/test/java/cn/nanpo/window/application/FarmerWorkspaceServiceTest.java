package cn.nanpo.window.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import cn.nanpo.window.api.farmer.FarmerViews.FarmRecordCommand;
import cn.nanpo.window.api.farmer.FarmerViews.FarmRecordView;
import cn.nanpo.window.api.farmer.FarmerViews.FarmerProfileView;
import cn.nanpo.window.common.error.ApiException;
import cn.nanpo.window.infrastructure.persistence.FarmerWorkspaceRepository;
import cn.nanpo.window.security.UserPrincipal;

class FarmerWorkspaceServiceTest {

    private static final long FARMER_ID = 1L;
    private static final long PRODUCT_ID = 1L;
    private static final long RECORD_ID = 9L;
    private static final Clock UTC_SERVER_CLOCK = Clock.fixed(
            Instant.parse("2026-08-29T16:30:00Z"), ZoneOffset.UTC);

    private FarmerWorkspaceService service;
    private UserPrincipal actor;

    @BeforeEach
    void setUp() {
        FarmerWorkspaceRepository repository = mock(FarmerWorkspaceRepository.class);
        AuditService auditService = mock(AuditService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        service = new FarmerWorkspaceService(repository, auditService, passwordEncoder, UTC_SERVER_CLOCK);
        actor = new UserPrincipal(2L, "13800000002", "村庄管理员", Set.of("SUPER_ADMIN"));

        when(repository.findFarmer(FARMER_ID)).thenReturn(Optional.of(new FarmerProfileView(
                FARMER_ID, "F001", "梁有福", "一组", "核桃种植户", "APPROVED")));
        when(repository.ownsProduct(FARMER_ID, PRODUCT_ID)).thenReturn(true);
        when(repository.createRecord(anyLong(), any(FarmRecordCommand.class))).thenReturn(RECORD_ID);
        when(repository.findRecord(FARMER_ID, RECORD_ID)).thenReturn(Optional.of(new FarmRecordView(
                RECORD_ID, PRODUCT_ID, "太行山核桃", null, null, "HARVEST",
                LocalDateTime.of(2026, 8, 30, 0, 30), "完成采收", null, true,
                "DRAFT", null, null, null, null, 0L,
                LocalDateTime.of(2026, 8, 30, 0, 30),
                LocalDateTime.of(2026, 8, 30, 0, 30), List.of())));
    }

    @Test
    void acceptsShanghaiLocalTimeWhenServerClockUsesUtc() {
        FarmRecordCommand command = commandAt(LocalDateTime.of(2026, 8, 30, 0, 30));

        assertDoesNotThrow(() -> service.createRecordForFarmer(FARMER_ID, command, actor, "127.0.0.1"));
    }

    @Test
    void rejectsTimeMoreThanFiveMinutesAheadInShanghai() {
        FarmRecordCommand command = commandAt(LocalDateTime.of(2026, 8, 30, 0, 36));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.createRecordForFarmer(FARMER_ID, command, actor, "127.0.0.1"));

        assertEquals("生产时间不能晚于当前时间", exception.getMessage());
    }

    private FarmRecordCommand commandAt(LocalDateTime occurredAt) {
        return new FarmRecordCommand(PRODUCT_ID, null, "HARVEST", occurredAt, "完成采收", true);
    }
}
