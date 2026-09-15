package br.com.sprint1.challenge.service;

public interface PasswordResetEmailService {
    void sendPasswordResetEmail(String recipient, String resetToken);
}
