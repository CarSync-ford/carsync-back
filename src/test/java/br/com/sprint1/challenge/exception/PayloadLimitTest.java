package br.com.sprint1.challenge.exception;

import br.com.sprint1.challenge.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PayloadLimitTest.TestUploadController.class)
class PayloadLimitTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtService jwtService;

    @TestConfiguration
    @RestController
    static class TestUploadController {
        @PostMapping("/api/v1/test-upload")
        public String upload(@RequestParam("file") MultipartFile file) {
            return "ok";
        }
    }

    @Test
    void shouldReturn413WhenFileExceedsLimit() {
        byte[] oversizedContent = new byte[1024 * 1024 + 1];

        ByteArrayResource resource = new ByteArrayResource(oversizedContent) {
            @Override
            public String getFilename() {
                return "large.bin";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(jwtService.generateToken("test-user", "test@example.com"));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/test-upload",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertTrue(response.getBody().contains("O tamanho do arquivo excede o limite permitido de 1MB."));
        assertTrue(response.getBody().contains("\"status\":413"));
    }
}
