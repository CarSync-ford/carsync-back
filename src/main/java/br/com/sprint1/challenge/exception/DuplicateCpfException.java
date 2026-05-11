package br.com.sprint1.challenge.exception;

public class DuplicateCpfException extends RuntimeException {
    public DuplicateCpfException(String cpf) {
        super("CPF já cadastrado: " + cpf);
    }
}