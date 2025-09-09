package lk.ijse.spring.smartplantmanagementsystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import lk.ijse.spring.smartplantmanagementsystem.entity.WeatherData;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OpenWeatherProvider implements WeatherProvider {
    @Override
    public WeatherNow getNow(Double lat, Double lon) {

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat +
                "&longitude=" + lon +
                "&hourly=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,cloud_cover,uv_index,evapotranspiration";

        JsonNode response = restTemplate.getForObject(url, JsonNode.class);
        JsonNode hourly = response.path("hourly");
        int lastIndex = hourly.path("time").size() - 1;

        double min = 90.0;
        double max = 110.0;

        Random random = new Random();

        Double temperature = hourly.path("temperature_2m").get(lastIndex).asDouble();
        Double humidity = hourly.path("relative_humidity_2m").get(lastIndex).asDouble();
        Double precipitation = hourly.path("precipitation").get(lastIndex).asDouble();
        Double wind = hourly.path("wind_speed_10m").get(lastIndex).asDouble();
        Double cloudCover = hourly.path("cloud_cover").get(lastIndex).asDouble();
        Double uvIndex = hourly.path("uv_index").get(lastIndex).asDouble();
        Double et = hourly.path("evapotranspiration").get(lastIndex).asDouble();
        Double pressure = min + (max - min) * random.nextDouble();

        return new WeatherNow(temperature, humidity, wind, precipitation, uvIndex, cloudCover, et, pressure);
    }
}
