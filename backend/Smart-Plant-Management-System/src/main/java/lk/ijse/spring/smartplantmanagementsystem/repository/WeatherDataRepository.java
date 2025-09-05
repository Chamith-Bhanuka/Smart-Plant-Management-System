package lk.ijse.spring.smartplantmanagementsystem.repository;

import lk.ijse.spring.smartplantmanagementsystem.entity.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {
    Optional<WeatherData> findTopByLocationIdOrderByTimestampDesc(Long locationId);
}
