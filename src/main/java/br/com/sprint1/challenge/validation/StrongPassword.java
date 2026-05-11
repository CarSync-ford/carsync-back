package br.com.sprint1.challenge.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "Senha deve ter pelo menos 8 caracteres, uma letra maiúscula e um caractere especial";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}