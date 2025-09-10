package lk.ijse.spring.smartplantmanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SensorDTO {
    private LocalDateTime timestamp;
    private Double airTemperature;   // °C (sensor)
    private Double airHumidity;      // %  (sensor)
    private Double soilMoisture;     // %
    private Double lightIntensity;   // lux
}
