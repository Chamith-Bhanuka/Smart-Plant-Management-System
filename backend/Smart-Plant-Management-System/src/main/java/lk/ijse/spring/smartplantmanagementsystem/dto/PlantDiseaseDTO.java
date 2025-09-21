package lk.ijse.spring.smartplantmanagementsystem.dto;

import java.time.LocalDateTime;

public record PlantDiseaseDTO(
        Long plantId,
        String label,
        double confidence,
        String imageUrl,
        LocalDateTime timestamp
) {}