package lk.ijse.spring.smartplantmanagementsystem.repository;

import lk.ijse.spring.smartplantmanagementsystem.entity.Plant;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlantRepository extends JpaRepository<Plant, Long> {
    List<Plant> findByUser(User user);

    Optional<Object> findByUserAndScientificNameIgnoreCase(User user, String name);

    Optional<Plant> findTopByUserOrderByIdDesc(User user);
}
