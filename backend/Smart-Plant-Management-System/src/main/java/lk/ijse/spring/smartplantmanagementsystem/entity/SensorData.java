package lk.ijse.spring.smartplantmanagementsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;
    private Double airTemperature;
    private Double airHumidity;
    private Double soilMoisture;
    private Double lightIntensity;

    @ManyToOne(fetch = FetchType.LAZY)
    private Plant plant;
}
