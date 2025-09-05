package lk.ijse.spring.smartplantmanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlantResponseDTO {
    private Long id;
    private String scientificName;
    private String commonName;
    private Double score;
    private String imagePath;

    private String userEmail;
    private String userRole;

    private Double latitude;
    private Double longitude;

    private String optimalPlantName;
    private Double idealTemperature;
    private Double idealHumidity;
    private Double idealRainfall;
    private String soilType;
    private String sunlightExposure;
}
