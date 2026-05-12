package br.com.sprint1.challenge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private int preAuthExpirationMinutes;
    private String issuer;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public int getPreAuthExpirationMinutes() { return preAuthExpirationMinutes; }
    public void setPreAuthExpirationMinutes(int preAuthExpirationMinutes) { this.preAuthExpirationMinutes = preAuthExpirationMinutes; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}