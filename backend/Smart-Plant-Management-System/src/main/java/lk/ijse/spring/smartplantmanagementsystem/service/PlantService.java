package lk.ijse.spring.smartplantmanagementsystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import lk.ijse.spring.smartplantmanagementsystem.entity.Plant;
import lk.ijse.spring.smartplantmanagementsystem.repository.PlantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class PlantService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${plantnet.api.key}")
    private String apiKey;

    @Value("${plantnet.api.url}")
    private String apiUrl;

    private final PlantRepository plantRepository;

    public PlantService(PlantRepository plantRepository) {
        this.plantRepository = plantRepository;
    }

    public Plant identifyAndSave(MultipartFile file) throws IOException {

        //save file locally
        String filePath = uploadDir + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());

        // Call PlantNet API
        RestTemplate restTemplate = new RestTemplate();

        String url = apiUrl + "?api-key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("images", new FileSystemResource(new File(filePath)));
        body.add("organs", "leaf");

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);

        // Parse response
        JsonNode suggestions = response.getBody().path("results");
        JsonNode bestMatch = suggestions.get(0);

        String scientificName = bestMatch.path("species").path("scientificNameWithoutAuthor").asText();
        String commonName = bestMatch.path("species").path("commonNames").get(0).asText("");
        Double score = bestMatch.path("score").asDouble();

        Plant plant = new Plant(
                null,
                scientificName,
                commonName,
                score,
                filePath,
                null,
                null,
                null
        );
        return plantRepository.save(plant);
    }
}
