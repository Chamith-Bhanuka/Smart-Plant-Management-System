package lk.ijse.spring.smartplantmanagementsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptimalConditions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String plantName;

    private Double idealTemperature;
    private Double idealHumidity;
    private Double idealRainfall;

    private String soilType;
    private String sunlightExposure;

    @OneToMany(mappedBy = "optimalConditions")
    private List<Plant> plants;
}
