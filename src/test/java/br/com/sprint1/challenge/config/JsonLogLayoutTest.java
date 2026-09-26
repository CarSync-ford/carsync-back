package br.com.sprint1.challenge.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class JsonLogLayoutTest {

    private JsonLogLayout layout;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        layout = new JsonLogLayout();
        layout.start();
        objectMapper = new ObjectMapper();
    }

    @Test
    void doLayout_producesValidSingleLineJson() throws Exception {
        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        when(event.getTimeStamp()).thenReturn(1700000000000L);
        when(event.getLevel()).thenReturn(Level.INFO);
        when(event.getLoggerName()).thenReturn("br.com.sprint1.test.Logger");
        when(event.getThreadName()).thenReturn("test-thread");
        when(event.getFormattedMessage()).thenReturn("Hello \"world\"\nNew line\tTabbed");

        String logLine = layout.doLayout(event);

        assertNotNull(logLine);
        assertTrue(logLine.endsWith("\n"));
        // Only one newline at the very end
        assertEquals(1, logLine.chars().filter(ch -> ch == '\n').count());

        JsonNode root = objectMapper.readTree(logLine);
        assertEquals("INFO", root.get("level").asText());
        assertEquals("br.com.sprint1.test.Logger", root.get("logger").asText());
        assertEquals("test-thread", root.get("thread").asText());
        assertEquals("Hello \"world\"\nNew line\tTabbed", root.get("message").asText());
        assertEquals("", root.get("exception").asText());
    }

    @Test
    void doLayout_masksSensitiveFields() throws Exception {
        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        when(event.getTimeStamp()).thenReturn(1700000000000L);
        when(event.getLevel()).thenReturn(Level.WARN);
        when(event.getLoggerName()).thenReturn("br.com.sprint1.test.Logger");
        when(event.getThreadName()).thenReturn("main");
        when(event.getFormattedMessage()).thenReturn("User failed with password: SuperSecret123 and token=\"xyz-abc-123\" cpf: 12345678900");

        String logLine = layout.doLayout(event);
        JsonNode root = objectMapper.readTree(logLine);
        String maskedMessage = root.get("message").asText();

        assertFalse(maskedMessage.contains("SuperSecret123"));
        assertFalse(maskedMessage.contains("xyz-abc-123"));
        assertFalse(maskedMessage.contains("12345678900"));
        assertTrue(maskedMessage.contains("password: ***"));
        assertTrue(maskedMessage.contains("token=\"***"));
        assertTrue(maskedMessage.contains("cpf: ***"));
    }

    @Test
    void doLayout_formatsExceptionAsEscapedString() throws Exception {
        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        when(event.getTimeStamp()).thenReturn(1700000000000L);
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getLoggerName()).thenReturn("br.com.sprint1.test.Logger");
        when(event.getThreadName()).thenReturn("main");
        when(event.getFormattedMessage()).thenReturn("Operation failed");

        RuntimeException cause = new RuntimeException("Root cause error with \"quotes\"");
        IThrowableProxy throwableProxy = new ThrowableProxy(cause);
        when(event.getThrowableProxy()).thenReturn(throwableProxy);

        String logLine = layout.doLayout(event);
        // Valid single line JSON
        assertEquals(1, logLine.chars().filter(ch -> ch == '\n').count());

        JsonNode root = objectMapper.readTree(logLine);
        assertEquals("Operation failed", root.get("message").asText());
        assertTrue(root.get("exception").asText().contains("Root cause error with \"quotes\""));
    }
}
