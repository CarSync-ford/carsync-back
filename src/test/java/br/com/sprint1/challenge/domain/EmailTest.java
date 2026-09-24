package br.com.sprint1.challenge.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTest {

    @Test
    void isLowercase_emailMinusculo_retornaTrue() {
        assertTrue(Email.isLowercase("user@example.com"));
    }

    @Test
    void isLowercase_emailComMaiuscula_retornaFalse() {
        assertFalse(Email.isLowercase("User@Example.com"));
    }

    @Test
    void isLowercase_nulo_retornaTrue() {
        assertTrue(Email.isLowercase(null));
    }

    @Test
    void of_emailValido_retornaValueObject() {
        Email email = Email.of("user@example.com");

        assertEquals("user@example.com", email.value());
        assertEquals("user@example.com", email.toString());
    }

    @Test
    void of_emailComMaiuscula_lancaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Email.of("User@Example.com"));
    }

    @Test
    void of_nulo_lancaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Email.of(null));
    }

    @Test
    void of_vazioOuEmBranco_lancaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Email.of(""));
        assertThrows(IllegalArgumentException.class, () -> Email.of("   "));
    }

    @Test
    void of_semArrobaOuDominio_lancaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Email.of("abc"));
        assertThrows(IllegalArgumentException.class, () -> Email.of("user@semdominio"));
        assertThrows(IllegalArgumentException.class, () -> Email.of("@example.com"));
    }

    @Test
    void equals_doisEmailsIguais_saoIguais() {
        assertEquals(Email.of("user@example.com"), Email.of("user@example.com"));
    }
}
