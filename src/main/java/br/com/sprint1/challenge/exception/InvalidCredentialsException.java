package br.com.sprint1.challenge.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Credenciais inválidas: Usuário não encontrado ou senha incorreta");
    }
}