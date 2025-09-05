package lk.ijse.spring.smartplantmanagementsystem.dto;

import lk.ijse.spring.smartplantmanagementsystem.entity.OptimalConditions;
import lombok.Data;

@Data
public class PlantMonitorDTO {
    private Long plantId;
    private String scientificName;
    private String commonName;
    private String imagePath;
    private Double Score;

    private Double latitude;
    private Double longitude;

    private WeatherDTO weather;
    private OptimalConditions optimal;

    private SensorDTO sensor;
}
