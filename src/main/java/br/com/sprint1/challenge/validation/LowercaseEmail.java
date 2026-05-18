package br.com.sprint1.challenge.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = LowercaseEmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface LowercaseEmail {
    String message() default "Email deve conter apenas caracteres minúsculos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
