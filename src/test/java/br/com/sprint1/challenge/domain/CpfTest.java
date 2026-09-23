package br.com.sprint1.challenge.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpfTest {

    @Test
    void isValid_cpfValido_retornaTrue() {
        assertTrue(Cpf.isValid("529.982.247-25"));
    }

    @Test
    void isValid_cpfComDigitosRepetidos_retornaFalse() {
        assertFalse(Cpf.isValid("111.111.111-11"));
    }

    @Test
    void isValid_cpfComDigitoVerificadorErrado_retornaFalse() {
        assertFalse(Cpf.isValid("529.982.247-00"));
    }

    @Test
    void isValid_nuloOuVazio_retornaFalse() {
        assertFalse(Cpf.isValid(null));
        assertFalse(Cpf.isValid(""));
        assertFalse(Cpf.isValid("   "));
    }

    @Test
    void of_cpfValido_retornaValueObjectComApenasDigitos() {
        Cpf cpf = Cpf.of("529.982.247-25");

        assertEquals("52998224725", cpf.digits());
        assertEquals("52998224725", cpf.toString());
    }

    @Test
    void of_cpfInvalido_lancaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Cpf.of("000.000.000-00"));
    }

    @Test
    void equals_doisCpfsComMesmosDigitos_saoIguais() {
        assertEquals(Cpf.of("52998224725"), Cpf.of("529.982.247-25"));
    }
}
