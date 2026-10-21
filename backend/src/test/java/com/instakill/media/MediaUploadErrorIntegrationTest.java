package com.instakill.media;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against a real embedded server, because errors raised before a controller is chosen
 * (multipart size limit, unsupported content type) go through the servlet container's /error
 * forwarding, which MockMvc does not simulate.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.servlet.multipart.max-file-size=1KB",
        "spring.servlet.multipart.max-request-size=1KB"
})
@ActiveProfiles("test")
class MediaUploadErrorIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String cookie;

    @BeforeEach
    void registerUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String payload = "{\"username\":\"limit" + suffix + "\",\"email\":\"limit" + suffix + "@example.com\",\"password\":\"password\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/register", new HttpEntity<>(payload, headers), String.class);
        cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    }

    @Test
    void oversizedUploadReturns413() {
        ResponseEntity<String> response = upload(new byte[5 * 1024]);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).contains("Uploaded file is too large");
    }

    @Test
    void smallUploadStillSucceeds() {
        ResponseEntity<String> response = upload(new byte[100]);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void nonMultipartBodyReturns415() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/media", new HttpEntity<>("{}", headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    private ResponseEntity<String> upload(byte[] bytes) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "clip.mp4";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add(HttpHeaders.COOKIE, cookie);
        return restTemplate.postForEntity("/api/media", new HttpEntity<>(body, headers), String.class);
    }
}
