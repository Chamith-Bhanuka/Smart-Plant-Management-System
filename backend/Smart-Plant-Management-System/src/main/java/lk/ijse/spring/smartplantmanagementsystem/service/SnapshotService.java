package lk.ijse.spring.smartplantmanagementsystem.service;

import lk.ijse.spring.smartplantmanagementsystem.dto.OptimalDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.PlantMonitorDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.SensorDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.WeatherDTO;
import lk.ijse.spring.smartplantmanagementsystem.entity.OptimalConditions;
import lk.ijse.spring.smartplantmanagementsystem.entity.Plant;
import lk.ijse.spring.smartplantmanagementsystem.entity.SensorData;
import lk.ijse.spring.smartplantmanagementsystem.entity.WeatherData;
import lk.ijse.spring.smartplantmanagementsystem.repository.PlantRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.SensorDataRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SnapshotService {

    private final PlantRepository plantRepository;
    private final WeatherDataRepository weatherDataRepository;
    private final SensorDataRepository sensorDataRepository;
    private final WeatherProvider weatherProvider;
    private final SensorProvider sensorProvider;

    public PlantMonitorDTO captureLatestSnapshot(Long plantId, String userEmail) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Plant not found"));

        //fetch live weather and save it
        var loc = plant.getLocation();
        WeatherProvider.WeatherNow now = weatherProvider.getNow(loc.getLatitude(), loc.getLongitude());
        WeatherData row = new WeatherData();
        row.setTimestamp(LocalDateTime.now());
        row.setTemperature(now.temperature());
        row.setHumidity(now.humidity());
        row.setWindSpeed(now.wind());
        row.setPrecipitation(now.precipitation());
        row.setUvIndex(now.uvIndex());
        row.setCloudCover(now.cloudCover());
        row.setEvapotranspiration(now.et());
        row.setPressure(now.pressure());
        row.setLocation(loc);
        weatherDataRepository.save(row);

        //fetch live sensor data and save it
        SensorProvider.SensorNow s = sensorProvider.getNow(plant.getId());
        SensorData sRow = new SensorData();
        sRow.setTimestamp(LocalDateTime.now());
        sRow.setAirTemperature(s.airTemperature());
        sRow.setAirHumidity(s.airHumidity());
        sRow.setSoilMoisture(s.soilMoisture());
        sRow.setLightIntensity(s.lightIntensity());
        sRow.setPlant(plant);
        sensorDataRepository.save(sRow);

        return mapToMonitorDTO(plant, row, sRow);
    }

    private PlantMonitorDTO mapToMonitorDTO(Plant plant, WeatherData latestW, SensorData latestS) {
        PlantMonitorDTO monitorDTO = new PlantMonitorDTO();
        monitorDTO.setPlantId(plant.getId());
        monitorDTO.setScientificName(plant.getScientificName());
        monitorDTO.setCommonName(plant.getCommonName());
        monitorDTO.setImagePath(plant.getImagePath());
        monitorDTO.setScore(plant.getScore());
        monitorDTO.setLatitude(plant.getLocation().getLatitude());
        monitorDTO.setLongitude(plant.getLocation().getLongitude());
        monitorDTO.setPlantedDate(plant.getPlantedDate());

        WeatherDTO w = new WeatherDTO();
        w.setTimestamp(latestW.getTimestamp());
        w.setAirTemperature(latestW.getTemperature());
        w.setAirHumidity(latestW.getHumidity());
        w.setWind(latestW.getWindSpeed());
        w.setPrecipitation(latestW.getPrecipitation());
        w.setUvIndex(latestW.getUvIndex());
        w.setCloudCover(latestW.getCloudCover());
        w.setEvapotranspiration(latestW.getEvapotranspiration());
        w.setPressure(latestW.getPressure());

        monitorDTO.setWeather(w);

        if (latestS != null) {
            SensorDTO s = new SensorDTO();
            s.setTimestamp(latestS.getTimestamp());
            s.setAirTemperature(latestS.getAirTemperature());
            s.setAirHumidity(latestS.getAirHumidity());
            s.setSoilMoisture(latestS.getSoilMoisture());
            s.setLightIntensity(latestS.getLightIntensity());

            monitorDTO.setSensor(s);
        }

        OptimalConditions oc = plant.getOptimalConditions();
        if (oc != null) {
            OptimalDTO o = new OptimalDTO();
            o.setPlantName(oc.getPlantName());
            o.setIdealTemperature(oc.getIdealTemperature());
            o.setIdealHumidity(oc.getIdealHumidity());
            o.setIdealRainfall(oc.getIdealRainfall());
            o.setSoilType(oc.getSoilType());
            o.setSunlightExposure(oc.getSunlightExposure());
            o.setDaysToHarvest(oc.getDaysToHarvest());
            o.setYieldPredictionKg(oc.getYieldPredictionKg());

            monitorDTO.setOptimal(o);
        }
        return monitorDTO;
    }
}
