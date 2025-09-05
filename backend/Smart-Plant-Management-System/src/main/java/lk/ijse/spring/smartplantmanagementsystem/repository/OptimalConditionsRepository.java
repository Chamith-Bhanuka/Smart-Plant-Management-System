package lk.ijse.spring.smartplantmanagementsystem.repository;

import lk.ijse.spring.smartplantmanagementsystem.entity.OptimalConditions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OptimalConditionsRepository extends JpaRepository<OptimalConditions, Long> {
    Optional<OptimalConditions> findByPlantNameIgnoreCase(String plantName);
}
