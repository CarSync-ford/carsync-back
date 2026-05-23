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
import java.util.Set;

@Component
public class HmacSignatureFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-HMAC-Signature";
    private static final String ALGORITHM = "HmacSHA256";
    private static final Set<String> PUBLIC_PREFIX_PATHS = Set.of(
        "/actuator/health/",
        "/api/v1/health",
        "/api/v1/auth",
        "/swagger-ui",
        "/v3/api-docs"
    );

    @Value("${hmac.secret:}")
    private String secret;

    @Value("${hmac.enabled:true}")
    private boolean enabled;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (PUBLIC_PREFIX_PATHS.stream().anyMatch(path::startsWith)) {
            return true;
        }
        // POST /api/v1/user (signup) is public, but GET /api/v1/user/me is not
        return "POST".equals(request.getMethod()) && "/api/v1/user".equals(path);
    }

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
