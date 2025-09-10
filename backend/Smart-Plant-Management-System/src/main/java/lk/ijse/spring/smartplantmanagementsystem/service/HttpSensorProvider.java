package lk.ijse.spring.smartplantmanagementsystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class HttpSensorProvider implements SensorProvider {

    @Value("${esp32.base.url}")
    private String esp32BaseUrl;

    @Value("${php.server.base.url}")
    private String phpServerBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public SensorNow getNow(Long plantId) {

        try {
            // fetch dht data
            JsonNode dht = restTemplate.getForObject(esp32BaseUrl + "/dht", JsonNode.class);
            double temp = dht.path("temperature").asDouble();
            double humidity = dht.path("humidity").asDouble();

            // fetch soil moisture
            JsonNode soil = restTemplate.getForObject(esp32BaseUrl + "/soil", JsonNode.class);
            double soilMoisture = soil.path("percent").asDouble();

            System.out.println("🌡️temp from snap: " + temp);
            System.out.println("🌧️humidity from snap: " + humidity);
            System.out.println("🌍soil from snap: " + soil);

            return new SensorNow(temp, humidity, soilMoisture, 900.0);

        } catch (Exception e) {
            //throw new RuntimeException("Failed to fetch sensor data", e);
            return new SensorNow(0.0, 0.0, 0.0, 0.0);
        } finally {
            System.out.println("esp32 base url: " + esp32BaseUrl);
            System.out.println("php server url: " + phpServerBaseUrl);
        }
    }
}
