package lk.ijse.spring.smartplantmanagementsystem.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WeatherDTO {
    private LocalDateTime timestamp;
    private Double airTemperature;
    private Double airHumidity;
    private Double wind;                // km/h
    private Double precipitation;       // mm
    private Double uvIndex;
    private Double cloudCover;          // %
    private Double evapotranspiration;  // mm/day
    private Double pressure;            // hPa
}
