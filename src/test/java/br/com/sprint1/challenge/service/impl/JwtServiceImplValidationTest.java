package br.com.sprint1.challenge.service.impl;

import br.com.sprint1.challenge.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplValidationTest {

    @Test
    void validateSecret_32chars_doesNotThrow() {
        JwtProperties props = mock(JwtProperties.class);
        when(props.getSecret()).thenReturn("exactly32characterslongsecretkey!");

        JwtServiceImpl service = new JwtServiceImpl(props, Clock.systemUTC());
        ReflectionTestUtils.invokeMethod(service, "validateSecret");

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateSecret"));
    }

    @Test
    void validateSecret_33chars_doesNotThrow() {
        JwtProperties props = mock(JwtProperties.class);
        when(props.getSecret()).thenReturn("exactly33characterslongsecretkey!a");

        JwtServiceImpl service = new JwtServiceImpl(props, Clock.systemUTC());
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateSecret"));
    }

    @Test
    void validateSecret_31chars_throwsIllegalStateException() {
        JwtProperties props = mock(JwtProperties.class);
        when(props.getSecret()).thenReturn("only31characterslongsecret!");

        JwtServiceImpl service = new JwtServiceImpl(props, Clock.systemUTC());
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> ReflectionTestUtils.invokeMethod(service, "validateSecret"));

        assert ex.getMessage().contains("256 bits");
        assert ex.getMessage().contains("32 characters");
    }

    @Test
    void validateSecret_empty_throwsIllegalStateException() {
        JwtProperties props = mock(JwtProperties.class);
        when(props.getSecret()).thenReturn("");

        JwtServiceImpl service = new JwtServiceImpl(props, Clock.systemUTC());
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> ReflectionTestUtils.invokeMethod(service, "validateSecret"));

        assert ex.getMessage().contains("256 bits");
    }

    @Test
    void validateSecret_null_throwsIllegalStateException() {
        JwtProperties props = mock(JwtProperties.class);
        when(props.getSecret()).thenReturn(null);

        JwtServiceImpl service = new JwtServiceImpl(props, Clock.systemUTC());
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> ReflectionTestUtils.invokeMethod(service, "validateSecret"));

        assert ex.getMessage().contains("256 bits");
    }

    @Test
    void validateSecret_unicodeChars_countAsBytes() {
        // Unicode chars may take more than 1 byte in UTF-8
        JwtProperties props = mock(JwtProperties.class);
        when(props.getSecret()).thenReturn("abcdefghijklmnopqrstuvwxyzçãõ"); // 32 chars but more bytes

        JwtServiceImpl service = new JwtServiceImpl(props, Clock.systemUTC());
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateSecret"));
    }
}