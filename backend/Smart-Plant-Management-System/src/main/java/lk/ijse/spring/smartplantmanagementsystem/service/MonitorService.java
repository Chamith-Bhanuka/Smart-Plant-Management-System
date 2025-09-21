package lk.ijse.spring.smartplantmanagementsystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityNotFoundException;
import lk.ijse.spring.smartplantmanagementsystem.dto.*;
import lk.ijse.spring.smartplantmanagementsystem.entity.Plant;
import lk.ijse.spring.smartplantmanagementsystem.entity.PlantDisease;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import lk.ijse.spring.smartplantmanagementsystem.entity.WeatherData;
import lk.ijse.spring.smartplantmanagementsystem.repository.PlantDiseaseRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.PlantRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.UserRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitorService {

    @Value("${esp32.base.url}")
    private String esp32BaseUrl;

    @Value("${php.server.base.url}")
    private String phpServerBaseUrl;

    @Value("${php.images-path}")
    private String imagesPath;

    @Value("${python.predict-url}")
    private String pythonPredictUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private final UserRepository userRepository;
    private final PlantRepository plantRepository;
    private final WeatherDataRepository weatherDataRepository;

    private final PlantDiseaseRepository diseaseRepository;

    public List<PlantMonitorDTO> getMonitorData(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        //List<Plant> plants = plantRepository.findByUser(user);
        List<Plant> plants = plantRepository.findTop1ByUserOrderByIdDesc(user);

        return plants.stream().map(this::toMonitorDTO).toList();
    }

    private PlantMonitorDTO toMonitorDTO(Plant plant) {
        PlantMonitorDTO dto = new PlantMonitorDTO();
        dto.setPlantId(plant.getId());
        dto.setScientificName(plant.getScientificName());
        dto.setCommonName(plant.getCommonName());
        dto.setImagePath(plant.getImagePath());
        dto.setScore(plant.getScore());
        dto.setPlantedDate(plant.getPlantedDate());

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

    private SensorDTO fetchLatestSensorTelemetry(Long plantId) {

        SensorDTO dto = new SensorDTO();

        try {
            // fetch dht data
            JsonNode dht = restTemplate.getForObject(esp32BaseUrl + "/dht", JsonNode.class);
            double temp = dht.path("temperature").asDouble();
            double humidity = dht.path("humidity").asDouble();

            // fetch soil moisture
            JsonNode soil = restTemplate.getForObject(esp32BaseUrl + "/soil", JsonNode.class);
            double soilMoisture = soil.path("percent").asDouble();

            dto.setTimestamp(LocalDateTime.now());
            dto.setAirTemperature(temp);
            dto.setAirHumidity(humidity);
            dto.setSoilMoisture(soilMoisture);
            dto.setLightIntensity(700.7);

            System.out.println("🌡️temp from monitor: " + temp);
            System.out.println("🌧️humidity from monitor: " + humidity);
            System.out.println("🌍soil from monitor: " + soil);

            return dto;

        } catch (Exception e) {
            //throw new RuntimeException("Failed to fetch sensor data", e);
            return new SensorDTO(LocalDateTime.now(),0.0, 0.0, 0.0, 0.0);
        } finally {
            System.out.println("esp32 base url: " + esp32BaseUrl);
            System.out.println("php server url: " + phpServerBaseUrl);
        }
    }


    public PlantDiseaseDTO checkForDisease(Long plantId) throws IOException {
        // 1. Validate plant
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new EntityNotFoundException("Plant not found"));

        // 2. Trigger ESP32 capture
        restTemplate.getForEntity(esp32BaseUrl + "/capture", String.class);

        // 3. List captures and pick newest
        String[] files = restTemplate.getForObject(
                phpServerBaseUrl + "/latest.php", String[].class);
        if (files == null || files.length == 0) {
            throw new IllegalStateException("No capture files found");
        }
        String filename = files[files.length - 1];

        // 4. Build public image URL
        String imageUrl = phpServerBaseUrl
                + imagesPath
                + "/"
                + URLEncoder.encode(filename, StandardCharsets.UTF_8);

        // 5. Download image bytes
        byte[] imageBytes = restTemplate.getForObject(imageUrl, byte[].class);
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalStateException("Captured image not available");
        }

        // 6. Call Python model service
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource resource = new ByteArrayResource(imageBytes) {
            @Override public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        ResponseEntity<PredictResponse> response = restTemplate.postForEntity(
                pythonPredictUrl, requestEntity, PredictResponse.class);
        PredictResponse pr = response.getBody();
        if (pr == null) {
            throw new IllegalStateException("Prediction service returned no data");
        }

        // 7. Persist diagnosis
        PlantDisease pd = new PlantDisease();
        pd.setPlant(plant);
        pd.setLabel(pr.label());
        pd.setConfidence(pr.confidence());
        pd.setImageUrl(imageUrl);
        diseaseRepository.save(pd);

        // 8. Map to DTO and return
        return new PlantDiseaseDTO(
                plantId,
                pr.label(),
                pr.confidence(),
                imageUrl,
                pd.getTimestamp()
        );
    }
}
