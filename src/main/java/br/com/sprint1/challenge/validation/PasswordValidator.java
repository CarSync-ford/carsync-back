package br.com.sprint1.challenge.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final int MIN_LENGTH = 8;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return false;
        }

        if (password.length() < MIN_LENGTH) {
            return false;
        }

        if (!password.chars().anyMatch(Character::isUpperCase)) {
            return false;
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            return false;
        }

        return true;
    }
}