package lk.ijse.spring.smartplantmanagementsystem.service;

import lk.ijse.spring.smartplantmanagementsystem.dto.OptimalDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.PlantMonitorDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.SensorDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.WeatherDTO;
import lk.ijse.spring.smartplantmanagementsystem.entity.Plant;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import lk.ijse.spring.smartplantmanagementsystem.entity.WeatherData;
import lk.ijse.spring.smartplantmanagementsystem.repository.PlantRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.UserRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitorService {

    private final UserRepository userRepository;
    private final PlantRepository plantRepository;
    private final WeatherDataRepository weatherDataRepository;

    public List<PlantMonitorDTO> getMonitorData(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Plant> plants = plantRepository.findByUser(user);

        return plants.stream().map(this::toMonitorDTO).toList();
    }

    private PlantMonitorDTO toMonitorDTO(Plant plant) {
        PlantMonitorDTO dto = new PlantMonitorDTO();
        dto.setPlantId(plant.getId());
        dto.setScientificName(plant.getScientificName());
        dto.setCommonName(plant.getCommonName());
        dto.setImagePath(plant.getImagePath());
        dto.setScore(plant.getScore());

        if (plant.getLocation() != null) {
            dto.setLatitude(plant.getLocation().getLatitude());
            dto.setLongitude(plant.getLocation().getLongitude());

            WeatherData latest = weatherDataRepository
                    .findTopByLocationIdOrderByTimestampDesc(plant.getLocation().getId())
                    .orElse(null);

            if (latest != null) {
                WeatherDTO w = new WeatherDTO();
                w.setTimestamp(latest.getTimestamp());
                w.setAirTemperature(latest.getTemperature());
                w.setAirHumidity(latest.getHumidity());
                w.setWind(latest.getWindSpeed());
                w.setPrecipitation(latest.getPrecipitation());
                w.setUvIndex(latest.getUvIndex());
                w.setCloudCover(latest.getCloudCover());
                w.setEvapotranspiration(latest.getEvapotranspiration());
//                w.setPressure(latest.getPressure());
                dto.setWeather(w);
            }
        }

        if (plant.getOptimalConditions() != null) {
            OptimalDTO o = new OptimalDTO();
            o.setPlantName(plant.getOptimalConditions().getPlantName());
            o.setIdealTemperature(plant.getOptimalConditions().getIdealTemperature());
            o.setIdealHumidity(plant.getOptimalConditions().getIdealHumidity());
            o.setIdealRainfall(plant.getOptimalConditions().getIdealRainfall());
            o.setSoilType(plant.getOptimalConditions().getSoilType());
            o.setSunlightExposure(plant.getOptimalConditions().getSunlightExposure());
            dto.setOptimal(o);
        }

        // Optional: fetch sensor telemetry (air temp, air humidity, soil moisture, light)
        // Replace with your actual sensor service or repository call.
        dto.setSensor(fetchLatestSensorTelemetry(plant.getId()));

        return dto;
    }

    // Stub: replace with real sensor integration
    private SensorDTO fetchLatestSensorTelemetry(Long plantId) {
        // Return null if you don’t have sensors yet
        return null;
    }
}
