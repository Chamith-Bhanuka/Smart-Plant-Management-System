package lk.ijse.spring.smartplantmanagementsystem.service;

import org.springframework.stereotype.Service;

@Service
public class OpenWeatherProvider implements WeatherProvider {
    @Override
    public WeatherNow getNow(Double lat, Double lon) {
        // Call your provider, parse fields
        // ... pseudo mapping ...
        return new WeatherNow(27.1, 83.0, 5.5, 0.2, 6.0, 45.0, 3.2, 1012.0);
    }
}
