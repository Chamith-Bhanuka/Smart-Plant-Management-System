package lk.ijse.spring.smartplantmanagementsystem.repository;

import lk.ijse.spring.smartplantmanagementsystem.entity.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {
}
