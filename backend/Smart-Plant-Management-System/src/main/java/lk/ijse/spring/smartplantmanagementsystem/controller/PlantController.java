package lk.ijse.spring.smartplantmanagementsystem.controller;

import lk.ijse.spring.smartplantmanagementsystem.dto.PlantResponseDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.PlantSetupDTO;
import lk.ijse.spring.smartplantmanagementsystem.entity.Plant;
import lk.ijse.spring.smartplantmanagementsystem.service.PlantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/plants")
@RequiredArgsConstructor
public class PlantController {

    private final PlantService plantService;

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

}
