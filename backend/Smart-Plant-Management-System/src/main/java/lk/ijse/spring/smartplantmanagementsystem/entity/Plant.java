package lk.ijse.spring.smartplantmanagementsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String scientificName;
    private String commonName;
    private Double score;
    private String imagePath;

    private LocalDate plantedDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne
    @JoinColumn(name = "optimal_conditions_id")
    private OptimalConditions optimalConditions;

    @PrePersist
    protected void onCreate() {
        if (plantedDate == null) {
            plantedDate = LocalDate.now();
        }
    }
}
