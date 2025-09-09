package lk.ijse.spring.smartplantmanagementsystem.controller;

import lk.ijse.spring.smartplantmanagementsystem.dto.PlantResponseDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.PlantSetupDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.PlantSummaryDTO;
import lk.ijse.spring.smartplantmanagementsystem.entity.Plant;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import lk.ijse.spring.smartplantmanagementsystem.repository.PlantRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.UserRepository;
import lk.ijse.spring.smartplantmanagementsystem.service.PlantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

}
