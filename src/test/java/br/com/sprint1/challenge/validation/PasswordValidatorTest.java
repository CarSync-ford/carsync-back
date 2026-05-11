package br.com.sprint1.challenge.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    @Test
    void senhaValida_retornaTrue() {
        assertTrue(validator.isValid("Senh@123", null));
        assertTrue(validator.isValid("Teste@12", null));
        assertTrue(validator.isValid("Abc@12345", null));
    }

    @Test
    void senhaMenorQue8Caracteres_retornaFalse() {
        assertFalse(validator.isValid("Senha1@", null));
        assertFalse(validator.isValid("Abc1@", null));
    }

    @Test
    void senhaSemMaiuscula_retornaFalse() {
        assertFalse(validator.isValid("senha123@", null));
        assertFalse(validator.isValid("minha@123", null));
    }

    @Test
    void senhaSemCaractereEspecial_retornaFalse() {
        assertFalse(validator.isValid("Senha1234", null));
        assertFalse(validator.isValid("MinhaSenha1", null));
    }

    @Test
    void senhaNulaOuVazia_retornaFalse() {
        assertFalse(validator.isValid(null, null));
        assertFalse(validator.isValid("", null));
    }
}