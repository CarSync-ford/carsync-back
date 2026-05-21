package br.com.sprint1.challenge.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class HmacSignatureFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-HMAC-Signature";
    private static final String ALGORITHM = "HmacSHA256";

    @Value("${hmac.secret:}")
    private String secret;

    @Value("${hmac.enabled:true}")
    private boolean enabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled || secret == null || secret.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String signature = request.getHeader(HEADER);
        if (signature == null || signature.isBlank()) {
            sendUnauthorized(response);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        byte[] body = cachedRequest.getCachedBody();

        String payload;
        if (body.length > 0) {
            payload = new String(body, cachedRequest.getCharacterEncoding() != null
                    ? cachedRequest.getCharacterEncoding() : "UTF-8");
        } else {
            String query = request.getQueryString();
            payload = request.getRequestURI() + (query != null ? "?" + query : "");
        }

        String expected = computeHmac(payload);
        if (expected == null || !MessageDigest.isEqual(expected.getBytes(), signature.getBytes())) {
            sendUnauthorized(response);
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(), ALGORITHM));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes()));
        } catch (Exception e) {
            return null;
        }
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Invalid or missing HMAC signature\"}");
    }
}
