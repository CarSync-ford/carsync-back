package br.com.sprint1.challenge.service;

public interface TotpService {

    String generateSecret();

    boolean verifyCode(String secret, String code);
}
