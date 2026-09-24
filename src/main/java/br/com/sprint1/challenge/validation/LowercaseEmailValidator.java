package br.com.sprint1.challenge.validation;

import br.com.sprint1.challenge.domain.Email;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LowercaseEmailValidator implements ConstraintValidator<LowercaseEmail, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return Email.isLowercase(value);
    }
}
