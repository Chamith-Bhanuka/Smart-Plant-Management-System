package lk.ijse.spring.smartplantmanagementsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.ijse.spring.smartplantmanagementsystem.dto.PlantResponseDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.PlantSetupDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.PlantSummaryDTO;
import lk.ijse.spring.smartplantmanagementsystem.entity.Plant;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import lk.ijse.spring.smartplantmanagementsystem.repository.PlantRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.UserRepository;
import lk.ijse.spring.smartplantmanagementsystem.service.OllamaClientService;
import lk.ijse.spring.smartplantmanagementsystem.service.PlantService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/plants")
@RequiredArgsConstructor
public class PlantController {

    private final PlantService plantService;

    private final UserRepository userRepository;
    private final PlantRepository plantRepository;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final OllamaClientService ollamaClient;

    @Value("${ai.model.answer}")
    private String answerModel;

    @PostMapping("/setup")
    public ResponseEntity<?> setupPlant(
            @RequestParam("image") MultipartFile image,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            PlantSetupDTO dto = new PlantSetupDTO();
            dto.setImage(image);
            dto.setLatitude(latitude);
            dto.setLongitude(longitude);

            Plant savedPlant = plantService.setupPlant(dto, userDetails.getUsername());
            PlantResponseDTO responseDTO = plantService.mapToDTO(savedPlant);

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to setup plant", "details", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<PlantSummaryDTO>> search(@RequestParam String q,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        List<Plant> results = plantRepository.searchForUser(user, q);

        List<PlantSummaryDTO> dtos = results.stream().map(p -> {
            PlantSummaryDTO dto = new PlantSummaryDTO();
            dto.setPlantId(p.getId());
            dto.setScientificName(p.getScientificName());
            dto.setCommonName(p.getCommonName());
            dto.setImagePath(p.getImagePath());
            dto.setLatitude(p.getLocation() != null ? p.getLocation().getLatitude() : null);
            dto.setLongitude(p.getLocation() != null ? p.getLocation().getLongitude() : null);
            dto.setPlantedDate(p.getPlantedDate());
            if (p.getOptimalConditions() != null) {
                dto.setDaysToHarvest(p.getOptimalConditions().getDaysToHarvest());
                dto.setYieldPredictionKg(p.getOptimalConditions().getYieldPredictionKg());
            }
            return dto;
        }).toList();

        return ResponseEntity.ok(dtos);
    }


    @GetMapping("/lookup")
    public ResponseEntity<List<Map<String, Object>>> lookupPlants(
            @RequestParam(defaultValue = "") String q,
            @AuthenticationPrincipal UserDetails userDetails) {


        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long userId = user.getId();


        String like = "%" + q.toLowerCase() + "%";
        String sql = """
            SELECT id, common_name, scientific_name, image_path
            FROM plant
            WHERE user_id = ?
              AND (LOWER(common_name) LIKE ? OR LOWER(scientific_name) LIKE ?)
            ORDER BY planted_date DESC
            LIMIT 100
            """;

        List<Map<String, Object>> plants =
                jdbcTemplate.queryForList(sql, userId, like, like);

        return ResponseEntity.ok(plants);
    }


    @GetMapping("/details/{plantId}")
    public ResponseEntity<Map<String, Object>> getPlantDetails(
            @PathVariable Long plantId,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {


        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long userId = user.getId();


        String detailSql = """
            SELECT p.id,
                   p.scientific_name,
                   p.common_name,
                   p.score,
                   p.image_path,
                   p.planted_date,
                   l.latitude,
                   l.longitude,
                   oc.plant_name      AS optimal_for,
                   oc.ideal_temperature,
                   oc.ideal_humidity,
                   oc.ideal_rainfall,
                   oc.soil_type,
                   oc.sunlight_exposure,
                   oc.days_to_harvest,
                   oc.yield_prediction_kg
            FROM plant p
            JOIN location l ON p.location_id = l.id
            JOIN optimal_conditions oc ON p.optimal_conditions_id = oc.id
            WHERE p.id = ? AND p.user_id = ?
            """;

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(detailSql, plantId, userId);

        if (rows.isEmpty()) {
            return ResponseEntity.notFound().build();
        }


        String jsonData = objectMapper.writeValueAsString(rows.get(0));
        String prompt = """
            You are a plant assistant. Summarize these plant details in a friendly paragraph:
            %s
            """.formatted(jsonData);

        String summary = ollamaClient.generate(answerModel, prompt).trim();
        
        return ResponseEntity.ok(Map.of(
                "data",   rows.get(0),
                "summary", summary
        ));
    }

}
