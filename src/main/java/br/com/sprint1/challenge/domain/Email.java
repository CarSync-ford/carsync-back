package br.com.sprint1.challenge.domain;

import java.util.regex.Pattern;

/**
 * Value Object para e-mail: garante o formato (RFC simplificado) e a regra de
 * minúsculas, para que uma instância nunca exista em estado inválido —
 * independente de quem a construiu já ter validado o DTO de origem ou não.
 */
public final class Email {

    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("E-mail não pode ser nulo ou vazio");
        }
        if (!FORMAT.matcher(rawValue).matches()) {
            throw new IllegalArgumentException("E-mail com formato inválido: " + rawValue);
        }
        if (!isLowercase(rawValue)) {
            throw new IllegalArgumentException("E-mail deve estar em minúsculas: " + rawValue);
        }
        return new Email(rawValue);
    }

    public static boolean isLowercase(String rawValue) {
        if (rawValue == null) {
            return true;
        }
        return rawValue.equals(rawValue.toLowerCase());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
