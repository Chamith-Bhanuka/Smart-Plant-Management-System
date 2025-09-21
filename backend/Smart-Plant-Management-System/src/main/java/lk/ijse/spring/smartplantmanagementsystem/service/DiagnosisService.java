package lk.ijse.spring.smartplantmanagementsystem.service;

import lk.ijse.spring.smartplantmanagementsystem.dto.DiagnosisResponse;
import lk.ijse.spring.smartplantmanagementsystem.entity.DiagnosisCase;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import lk.ijse.spring.smartplantmanagementsystem.repository.DiagnosisCaseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

@Service
public class DiagnosisService {

    private final DiagnosisCaseRepository repo;
    private final OllamaClientService ollama;
    private final RestTemplate restTemplate = new RestTemplate();
    private final FileStorageService fileStorageService;

    @Value("${python.predict-url}")
    private String predictUrl;

    @Value("${ai.model.answer}")
    private String answerModel;

    public DiagnosisService(
            DiagnosisCaseRepository repo,
            OllamaClientService ollama,
            FileStorageService fileStorageService
    ) {
        this.repo               = repo;
        this.ollama             = ollama;
        this.fileStorageService = fileStorageService;
    }

    public DiagnosisResponse diagnose(MultipartFile file, User user) throws IOException {
        // 1) Store file safely via FileStorageService
        String filename = fileStorageService.store(file);

        // 2) Call Python /predict
        HttpHeaders pyHeaders = new HttpHeaders();
        pyHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource imgRes = new ByteArrayResource(file.getBytes()) {
            @Override public String getFilename() { return filename; }
        };
        MultiValueMap<String,Object> pyBody = new LinkedMultiValueMap<>();
        pyBody.add("file", imgRes);

        HttpEntity<MultiValueMap<String,Object>> pyReq =
                new HttpEntity<>(pyBody, pyHeaders);

        @SuppressWarnings("unchecked")
        Map<String,Object> pyResp = restTemplate.postForObject(predictUrl, pyReq, Map.class);

        String label      = (String) pyResp.get("label");
        double confidence = ((Number) pyResp.get("confidence")).doubleValue();

        // 3) Generate insights via Ollama
        String prompt = "You are a plant assistant. The model predicts \"" + label +
                "\". Provide concise treatment recommendations in 2–3 sentences.";
        String insights = ollama.generate(answerModel, prompt).trim();

        // 4) Build unique case code
        String datePart   = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", new Random().nextInt(10_000));
        String caseCode   = "SF" + datePart + randomPart;

        // 5) Persist DiagnosisCase
        DiagnosisCase dc = new DiagnosisCase();
        dc.setCaseCode(caseCode);
        dc.setFilename(filename);
        dc.setLabel(label);
        dc.setConfidence(confidence);
        dc.setInsights(insights);
        dc.setTimestamp(LocalDateTime.now());
        dc.setUser(user);
        repo.save(dc);

        // 6) Return DTO with public URL
        String imageUrl = "/uploads/diagnoses/" + filename;
        return new DiagnosisResponse(caseCode, imageUrl, label, confidence, insights, dc.getTimestamp());
    }
}
