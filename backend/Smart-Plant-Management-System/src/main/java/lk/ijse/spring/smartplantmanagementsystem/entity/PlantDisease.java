package lk.ijse.spring.smartplantmanagementsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "plant_disease")
@Getter
@Setter
public class PlantDisease {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Plant plant;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
