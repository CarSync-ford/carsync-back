package br.com.sprint1.challenge.exception;

public class DuplicateCpfException extends RuntimeException {
    public DuplicateCpfException() {
        super("CPF já cadastrado: ");
    }
}