package lk.ijse.spring.smartplantmanagementsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;

    private Double temperature; // °C
    private Double humidity; // %
    private Double precipitation; // mm
    private Double windSpeed; // m/s
    private Double windGusts; // m/s
    private Double cloudCover; // %
    private Double uvIndex;
    private Double evapotranspiration; // mm
    private Double pressure;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;
}
