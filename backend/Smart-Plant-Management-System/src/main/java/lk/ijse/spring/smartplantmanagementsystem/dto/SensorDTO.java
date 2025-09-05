package lk.ijse.spring.smartplantmanagementsystem.dto;

import lombok.Data;

@Data
public class SensorDTO {
    private Double airTemperature;   // °C (sensor)
    private Double airHumidity;      // %  (sensor)
    private Double soilMoisture;     // %
    private Double lightIntensity;   // lux
}
