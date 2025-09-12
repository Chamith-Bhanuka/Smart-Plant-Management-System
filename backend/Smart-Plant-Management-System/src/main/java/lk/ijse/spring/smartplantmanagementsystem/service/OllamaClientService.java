package lk.ijse.spring.smartplantmanagementsystem.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OllamaClientService {

    private static final Logger log = LoggerFactory.getLogger(OllamaClientService.class);

    @Value("${ollama.base-url}")
    private String baseUrl;
    private final RestTemplate restTemplate;

    public String generate(String model, String prompt) {
        int attempts = 0;
        while (attempts < 2) {
            attempts++;
            try {
                return callOllama(model, prompt);
            } catch (ResourceAccessException ex) {
                log.warn("Ollama call timed out (attempt {}/2). Retrying…", attempts);
                if (attempts >= 2) {
                    log.error("Ollama still not responding after {} attempts", attempts);
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("Unreachable");
    }

    private String callOllama(String model, String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", prompt,
                "temperature", prompt.contains("```sql") ? 0 : 0.7,
                "max_tokens", prompt.contains("```sql") ? 256 : 512,
                "stream", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                baseUrl + "/api/generate",
                new HttpEntity<>(body, headers),
                Map.class
        );

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("Ollama error: " + resp.getStatusCode());
        }

        String answer = Objects.toString(resp.getBody().get("response"), "");
        log.debug("Ollama response ({} chars)", answer.length());
        return answer;
    }
}
