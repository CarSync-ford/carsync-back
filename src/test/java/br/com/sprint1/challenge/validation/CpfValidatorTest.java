package br.com.sprint1.challenge.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CpfValidatorTest {

    private final CpfValidator validator = new CpfValidator();

    @Test
    void cpfValido_retornaTrue() {
        assertTrue(validator.isValid("52998224725", null));
        assertTrue(validator.isValid("123.456.789-09", null));
    }

    @Test
    void cpfComDigitosVerificadoresErrados_retornaFalse() {
        assertFalse(validator.isValid("52998224700", null));
    }

    @Test
    void cpfComTodosDigitosIguais_retornaFalse() {
        assertFalse(validator.isValid("11111111111", null));
        assertFalse(validator.isValid("00000000000", null));
    }

    @Test
    void cpfComTamanhoIncorreto_retornaFalse() {
        assertFalse(validator.isValid("123456789", null));
        assertFalse(validator.isValid("12345678901", null));
    }

    @Test
    void cpfNuloOuVazio_retornaFalse() {
        assertFalse(validator.isValid(null, null));
        assertFalse(validator.isValid("", null));
    }
}