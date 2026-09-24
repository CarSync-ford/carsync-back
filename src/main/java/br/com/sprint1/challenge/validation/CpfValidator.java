package br.com.sprint1.challenge.validation;

import br.com.sprint1.challenge.domain.Cpf;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<ValidCpf, String> {

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        return Cpf.isValid(cpf);
    }
}