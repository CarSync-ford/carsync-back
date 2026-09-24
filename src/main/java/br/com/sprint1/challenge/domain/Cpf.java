package br.com.sprint1.challenge.domain;

/**
 * Value Object para CPF: encapsula o algoritmo de validação (formato + dígitos
 * verificadores) num tipo imutável, em vez de espalhar a regra como string solta.
 */
public final class Cpf {

    private final String digits;

    private Cpf(String digits) {
        this.digits = digits;
    }

    public static Cpf of(String rawValue) {
        if (!isValid(rawValue)) {
            throw new IllegalArgumentException("CPF inválido: " + rawValue);
        }
        return new Cpf(rawValue.replaceAll("\\D", ""));
    }

    public static boolean isValid(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return false;
        }

        String digits = rawValue.replaceAll("\\D", "");

        if (digits.length() != 11) {
            return false;
        }

        if (digits.matches("(\\d)\\1{10}")) {
            return false;
        }

        return hasValidCheckDigits(digits);
    }

    private static boolean hasValidCheckDigits(String cpf) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
        }
        int remainder = sum % 11;
        int digit1 = (remainder < 2) ? 0 : 11 - remainder;
        if (digit1 != Character.getNumericValue(cpf.charAt(9))) {
            return false;
        }

        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
        }
        remainder = sum % 11;
        int digit2 = (remainder < 2) ? 0 : 11 - remainder;
        return digit2 == Character.getNumericValue(cpf.charAt(10));
    }

    public String digits() {
        return digits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cpf other)) return false;
        return digits.equals(other.digits);
    }

    @Override
    public int hashCode() {
        return digits.hashCode();
    }

    @Override
    public String toString() {
        return digits;
    }
}
