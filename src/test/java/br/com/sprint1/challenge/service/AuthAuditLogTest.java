package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.AuthDtos.ChangePasswordRequest;
import br.com.sprint1.challenge.entity.User;
import br.com.sprint1.challenge.repository.UserRepository;
import br.com.sprint1.challenge.service.impl.AuthServiceImpl;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthAuditLogTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private TotpService totpService;

    private AuthServiceImpl authService;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger authLogger;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, jwtService, totpService, 10);
        authLogger = (Logger) LoggerFactory.getLogger(AuthServiceImpl.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        authLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        authLogger.detachAppender(listAppender);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void rollback_doesNotEmitAuditLog() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            User user = new User();
            user.setId("u-123");
            user.setEmail("user@example.com");
            user.setHashedPassword(BCrypt.hashpw("oldPass123!", BCrypt.gensalt(10)));

            when(userRepository.findById("u-123")).thenReturn(Optional.of(user));

            authService.changePassword("u-123", new ChangePasswordRequest("oldPass123!", "newPass123!"));

            // Synchronization registered, but not committed
            assertTrue(listAppender.list.stream()
                    .noneMatch(event -> event.getMessage().contains("SECURITY_AUDIT")));

            // Simulate transaction rollback (afterCompletion with STATUS_ROLLED_BACK, without afterCommit)
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }

            // Verify still no audit log emitted
            assertTrue(listAppender.list.stream()
                    .noneMatch(event -> event.getMessage().contains("SECURITY_AUDIT")));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void commit_emitsAuditLogWithoutSecretsOrPii() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            User user = new User();
            user.setId("u-123");
            user.setEmail("user@example.com");
            user.setHashedPassword(BCrypt.hashpw("oldPass123!", BCrypt.gensalt(10)));

            when(userRepository.findById("u-123")).thenReturn(Optional.of(user));

            authService.changePassword("u-123", new ChangePasswordRequest("oldPass123!", "newPass123!"));

            // Simulate commit
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }

            // Verify audit log emitted
            var auditEvents = listAppender.list.stream()
                    .filter(event -> event.getMessage().contains("SECURITY_AUDIT"))
                    .toList();

            assertEquals(1, auditEvents.size());
            String logMsg = auditEvents.get(0).getMessage();
            assertEquals("SECURITY_AUDIT action:PASSWORD_CHANGE status:SUCCESS", logMsg);

            // Verify no PII / secrets
            assertFalse(logMsg.contains("user@example.com"));
            assertFalse(logMsg.contains("u-123"));
            assertFalse(logMsg.contains("oldPass123!"));
            assertFalse(logMsg.contains("newPass123!"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
