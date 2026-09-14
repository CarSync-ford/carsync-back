package br.com.sprint1.challenge.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DataMaskerTest {

    @Test
    @DisplayName("maskCpf should mask formatted 11-digit CPF")
    void maskCpf_formatted() {
        assertEquals("***.***.***-**", DataMasker.maskCpf("123.456.789-00"));
    }

    @Test
    @DisplayName("maskCpf should mask raw 11-digit CPF")
    void maskCpf_rawDigits() {
        assertEquals("***.***.***-**", DataMasker.maskCpf("12345678900"));
    }

    @Test
    @DisplayName("maskCpf with non-11 digits should mask digits with asterisk")
    void maskCpf_nonStandardDigits() {
        assertEquals("ABC-***-DEF", DataMasker.maskCpf("ABC-123-DEF"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("maskCpf should return unchanged for null or blank input")
    void maskCpf_nullOrBlank(String input) {
        assertEquals(input, DataMasker.maskCpf(input));
    }

    @Test
    @DisplayName("maskEmail should mask local part keeping first character")
    void maskEmail_standard() {
        assertEquals("u***@domain.com", DataMasker.maskEmail("user@domain.com"));
        assertEquals("j***@empresa.com.br", DataMasker.maskEmail("joao.silva@empresa.com.br"));
    }

    @Test
    @DisplayName("maskEmail should mask single-char local part")
    void maskEmail_singleCharLocal() {
        assertEquals("a***@domain.com", DataMasker.maskEmail("a@domain.com"));
    }

    @Test
    @DisplayName("maskEmail without at-sign or starting with at-sign should return original")
    void maskEmail_invalidFormat() {
        assertEquals("notanemail", DataMasker.maskEmail("notanemail"));
        assertEquals("@domain.com", DataMasker.maskEmail("@domain.com"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("maskEmail should return unchanged for null or blank input")
    void maskEmail_nullOrBlank(String input) {
        assertEquals(input, DataMasker.maskEmail(input));
    }

    @Test
    @DisplayName("maskPhone should mask standard 10 or 11 digits phone numbers")
    void maskPhone_standard() {
        assertEquals("(**) ****-****", DataMasker.maskPhone("(11) 99999-9999"));
        assertEquals("(**) ****-****", DataMasker.maskPhone("11999999999"));
        assertEquals("(**) ****-****", DataMasker.maskPhone("(11) 3333-4444"));
        assertEquals("(**) ****-****", DataMasker.maskPhone("1133334444"));
    }

    @Test
    @DisplayName("maskPhone with non-standard digit count should mask digits")
    void maskPhone_nonStandard() {
        assertEquals("tel: ***", DataMasker.maskPhone("tel: 123"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n"})
    @DisplayName("maskPhone should return unchanged for null or blank input")
    void maskPhone_nullOrBlank(String input) {
        assertEquals(input, DataMasker.maskPhone(input));
    }

    @Test
    @DisplayName("maskName should mask first and last names keeping first letters")
    void maskName_multipleNames() {
        assertEquals("J*** S****", DataMasker.maskName("João Silva"));
        assertEquals("M**** S***** O*******", DataMasker.maskName("Maria Santos Oliveira"));
    }

    @Test
    @DisplayName("maskName should handle single name and extra spaces")
    void maskName_singleNameAndSpaces() {
        assertEquals("C*****", DataMasker.maskName("Carlos"));
        assertEquals("A** P****", DataMasker.maskName("   Ana    Paula   "));
    }

    @Test
    @DisplayName("maskName should handle single character tokens")
    void maskName_singleCharTokens() {
        assertEquals("A B C", DataMasker.maskName("A B C"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\r\n"})
    @DisplayName("maskName should return unchanged for null or blank input")
    void maskName_nullOrBlank(String input) {
        assertEquals(input, DataMasker.maskName(input));
    }
}
