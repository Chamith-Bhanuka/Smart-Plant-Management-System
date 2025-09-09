package lk.ijse.spring.smartplantmanagementsystem.dto;

import lombok.Data;

@Data
public class PlantChatDTO {
    private Long plantId;
    private String scientificName;
    private String commonName;
    private String imagePath;
    private Double score;

    private Double latitude;
    private Double longitude;

    private WeatherDTO weather;
    private OptimalDTO optimal;
    private SensorDTO sensor;
}
