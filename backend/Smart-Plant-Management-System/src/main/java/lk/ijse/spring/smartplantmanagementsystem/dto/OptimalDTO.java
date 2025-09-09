package lk.ijse.spring.smartplantmanagementsystem.dto;

import lombok.Data;

@Data
public class OptimalDTO {
    private String plantName;
    private Double idealTemperature;
    private Double idealHumidity;
    private Double idealRainfall;
    private String soilType;
    private String sunlightExposure;
    private Integer daysToHarvest;
    private Double yieldPredictionKg;
}
