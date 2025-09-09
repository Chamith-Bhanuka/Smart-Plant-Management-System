package lk.ijse.spring.smartplantmanagementsystem.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PlantSummaryDTO {
    private Long plantId;
    private String scientificName;
    private String commonName;
    private String imagePath;
    private Double latitude;
    private Double longitude;
    private LocalDate plantedDate;      // new
    private Integer daysToHarvest;      // from Optimal
    private Double yieldPredictionKg;   // from Optimal
}
