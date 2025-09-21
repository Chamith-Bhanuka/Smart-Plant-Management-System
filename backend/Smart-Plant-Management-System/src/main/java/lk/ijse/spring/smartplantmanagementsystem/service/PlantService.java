package lk.ijse.spring.smartplantmanagementsystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import lk.ijse.spring.smartplantmanagementsystem.dto.*;
import lk.ijse.spring.smartplantmanagementsystem.entity.*;
import lk.ijse.spring.smartplantmanagementsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class PlantService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${plantnet.api.key}")
    private String apiKey;

    @Value("${plantnet.api.url}")
    private String apiUrl;

    private final PlantRepository plantRepository;
    private final LocationRepository locationRepository;
    private final WeatherDataRepository weatherDataRepository;
    private final OptimalConditionsRepository optimalConditionsRepository;
    private final UserRepository userRepository;

    public Plant setupPlant(PlantSetupDTO dto, String userEmail) throws IOException {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Save image locally
        String filePath = uploadDir + System.currentTimeMillis() + "_" + dto.getImage().getOriginalFilename();
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, dto.getImage().getBytes());

        // Identify plant via PlantNet
        JsonNode bestMatch = callPlantNet(filePath);
        String scientificName = bestMatch.path("species").path("scientificNameWithoutAuthor").asText();
        String commonName = bestMatch.path("species").path("commonNames").get(0).asText("");
        Double score = bestMatch.path("score").asDouble();

        // Match OptimalConditions by scientific name
        OptimalConditions conditions = optimalConditionsRepository.findByPlantNameIgnoreCase(scientificName)
                .orElseThrow(() -> new RuntimeException("No optimal conditions found for: " + scientificName));

        // Create location
        Location location = new Location();
        location.setLatitude(dto.getLatitude());
        location.setLongitude(dto.getLongitude());
        locationRepository.save(location);

        // Fetch latest weather data
        WeatherData weather = fetchLatestWeather(dto.getLatitude(), dto.getLongitude());
        weather.setLocation(location);
        weatherDataRepository.save(weather);

        // Create and save plant
        Plant plant = new Plant();
        plant.setScientificName(scientificName);
        plant.setCommonName(commonName);
        plant.setScore(score);
        plant.setImagePath(filePath);
        plant.setUser(user);
        plant.setLocation(location);
        plant.setOptimalConditions(conditions);

        return plantRepository.save(plant);
    }

    private JsonNode callPlantNet(String filePath) {
        RestTemplate restTemplate = new RestTemplate();
        String url = apiUrl + "?api-key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("images", new FileSystemResource(new File(filePath)));
        body.add("organs", "leaf");

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);

        return response.getBody().path("results").get(0);
    }

    private WeatherData fetchLatestWeather(Double lat, Double lon) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat +
                "&longitude=" + lon +
                "&hourly=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,cloud_cover,uv_index,evapotranspiration";

        JsonNode response = restTemplate.getForObject(url, JsonNode.class);
        JsonNode hourly = response.path("hourly");
        int lastIndex = hourly.path("time").size() - 1;

        WeatherData data = new WeatherData();
        data.setTimestamp(LocalDateTime.parse(hourly.path("time").get(lastIndex).asText()));
        data.setTemperature(hourly.path("temperature_2m").get(lastIndex).asDouble());
        data.setHumidity(hourly.path("relative_humidity_2m").get(lastIndex).asDouble());
        data.setPrecipitation(hourly.path("precipitation").get(lastIndex).asDouble());
        data.setWindSpeed(hourly.path("wind_speed_10m").get(lastIndex).asDouble());
        data.setCloudCover(hourly.path("cloud_cover").get(lastIndex).asDouble());
        data.setUvIndex(hourly.path("uv_index").get(lastIndex).asDouble());
        data.setEvapotranspiration(hourly.path("evapotranspiration").get(lastIndex).asDouble());

        return data;
    }

    public PlantResponseDTO mapToDTO(Plant plant) {
        PlantResponseDTO dto = new PlantResponseDTO();

        dto.setId(plant.getId());
        dto.setScientificName(plant.getScientificName());
        dto.setCommonName(plant.getCommonName());
        dto.setScore(plant.getScore());
        dto.setImagePath(plant.getImagePath());

        dto.setUserEmail(plant.getUser().getEmail());
        dto.setUserRole(plant.getUser().getRole().name());

        if (plant.getLocation() != null) {
            dto.setLatitude(plant.getLocation().getLatitude());
            dto.setLongitude(plant.getLocation().getLongitude());
        }

        if (plant.getOptimalConditions() != null) {
            dto.setOptimalPlantName(plant.getOptimalConditions().getPlantName());
            dto.setIdealTemperature(plant.getOptimalConditions().getIdealTemperature());
            dto.setIdealHumidity(plant.getOptimalConditions().getIdealHumidity());
            dto.setIdealRainfall(plant.getOptimalConditions().getIdealRainfall());
            dto.setSoilType(plant.getOptimalConditions().getSoilType());
            dto.setSunlightExposure(plant.getOptimalConditions().getSunlightExposure());
        }

        return dto;
    }

    public PlantChatDTO getPlantChatData(String name, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Plant plant = (Plant) plantRepository.findByUserAndScientificNameIgnoreCase(user, name)
                .orElseThrow(() -> new RuntimeException("Plant not found"));

        PlantChatDTO dto = new PlantChatDTO();
        dto.setPlantId(plant.getId());
        dto.setScientificName(plant.getScientificName());
        dto.setCommonName(plant.getCommonName());
        dto.setImagePath(plant.getImagePath());
        dto.setScore(plant.getScore());
        dto.setLatitude(plant.getLocation().getLatitude());
        dto.setLongitude(plant.getLocation().getLongitude());

        dto.setWeather(mapWeather(plant.getLocation()));
        dto.setOptimal(mapOptimal(plant.getOptimalConditions()));
        dto.setSensor(mapSensor(plant.getId())); // optional

        return dto;
    }

    private WeatherDTO mapWeather(Location location) {
        if (location == null || location.getWeatherHistory().isEmpty()) return null;

        WeatherData latest = location.getWeatherHistory().stream()
                .max(Comparator.comparing(WeatherData::getTimestamp))
                .orElse(null);

        if (latest == null) return null;

        WeatherDTO dto = new WeatherDTO();
        dto.setTimestamp(latest.getTimestamp());
        dto.setAirTemperature(latest.getTemperature());
        dto.setAirHumidity(latest.getHumidity());
        dto.setWind(latest.getWindSpeed());
        dto.setPrecipitation(latest.getPrecipitation());
        dto.setUvIndex(latest.getUvIndex());
        dto.setCloudCover(latest.getCloudCover());
        dto.setEvapotranspiration(latest.getEvapotranspiration());
        return dto;
    }

    private OptimalDTO mapOptimal(OptimalConditions optimal) {
        if (optimal == null) return null;

        OptimalDTO dto = new OptimalDTO();
        dto.setPlantName(optimal.getPlantName());
        dto.setIdealTemperature(optimal.getIdealTemperature());
        dto.setIdealHumidity(optimal.getIdealHumidity());
        dto.setIdealRainfall(optimal.getIdealRainfall());
        dto.setSoilType(optimal.getSoilType());
        dto.setSunlightExposure(optimal.getSunlightExposure());
        return dto;
    }

    private SensorDTO mapSensor(Long plantId) {
        // TODO: Replace with actual sensor integration
        return null;
    }

}
