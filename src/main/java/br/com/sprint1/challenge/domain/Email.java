package br.com.sprint1.challenge.domain;

/**
 * Value Object para e-mail: hoje só garante a regra de minúsculas já aplicada
 * pela validação existente, num tipo imutável em vez de checagem solta em String.
 */
public final class Email {

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String rawValue) {
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
