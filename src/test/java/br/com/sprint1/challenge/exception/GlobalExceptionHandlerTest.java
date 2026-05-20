package br.com.sprint1.challenge.exception;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).detachAppender(logAppender);
    }

    @Test
    void handleGeneric_logsInternalError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");

        handler.handleGeneric(new RuntimeException("unexpected"), request);

        assertTrue(logAppender.list.stream()
                .anyMatch(e -> e.getLevel().toString().equals("ERROR")
                        && e.getFormattedMessage().contains("INTERNAL_ERROR")));
    }

    @Test
    void handleDuplicateCpf_logsDuplicateRegistration() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users");

        handler.handleDuplicateCpf(new DuplicateCpfException(), request);

        assertTrue(logAppender.list.stream()
                .anyMatch(e -> e.getLevel().toString().equals("WARN")
                        && e.getFormattedMessage().contains("DUPLICATE_REGISTRATION")
                        && e.getFormattedMessage().contains("type=cpf")));
    }

    @Test
    void handleDuplicateEmail_logsDuplicateRegistration() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users");

        handler.handleDuplicateEmail(new DuplicateEmailException(), request);

        assertTrue(logAppender.list.stream()
                .anyMatch(e -> e.getLevel().toString().equals("WARN")
                        && e.getFormattedMessage().contains("DUPLICATE_REGISTRATION")
                        && e.getFormattedMessage().contains("type=email")));
    }
}
