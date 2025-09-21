package lk.ijse.spring.smartplantmanagementsystem.repository;

import lk.ijse.spring.smartplantmanagementsystem.entity.Plant;
import lk.ijse.spring.smartplantmanagementsystem.entity.PlantDisease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlantDiseaseRepository extends JpaRepository<PlantDisease, Long> {
    List<PlantDisease> findTop1ByPlantOrderByTimestampDesc(Plant plant);
}
