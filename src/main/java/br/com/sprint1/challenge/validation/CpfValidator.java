package br.com.sprint1.challenge.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<ValidCpf, String> {

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        if (cpf == null || cpf.isBlank()) {
            return false;
        }

        String digits = cpf.replaceAll("\\D", "");

        if (digits.length() != 11) {
            return false;
        }

        if (digits.matches("(\\d)\\1{10}")) {
            return false;
        }

        return validateCheckDigits(digits);
    }

    private boolean validateCheckDigits(String cpf) {
        int sum;

        // First check digit
        sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
        }
        int remainder = sum % 11;
        int digit1 = (remainder < 2) ? 0 : 11 - remainder;
        if (digit1 != Character.getNumericValue(cpf.charAt(9))) {
            return false;
        }

        // Second check digit
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
        }
        remainder = sum % 11;
        int digit2 = (remainder < 2) ? 0 : 11 - remainder;
        return digit2 == Character.getNumericValue(cpf.charAt(10));
    }
}