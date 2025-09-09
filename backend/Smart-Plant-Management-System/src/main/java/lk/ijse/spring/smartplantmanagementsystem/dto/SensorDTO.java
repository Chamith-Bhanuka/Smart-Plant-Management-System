package lk.ijse.spring.smartplantmanagementsystem.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SensorDTO {
    private LocalDateTime timestamp;
    private Double airTemperature;   // °C (sensor)
    private Double airHumidity;      // %  (sensor)
    private Double soilMoisture;     // %
    private Double lightIntensity;   // lux
}
