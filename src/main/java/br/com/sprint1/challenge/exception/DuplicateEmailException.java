package br.com.sprint1.challenge.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("Email já cadastrado ");
    }  
}
