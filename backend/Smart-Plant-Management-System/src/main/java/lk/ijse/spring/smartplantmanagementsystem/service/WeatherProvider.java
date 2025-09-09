package lk.ijse.spring.smartplantmanagementsystem.service;

public interface WeatherProvider {
    record WeatherNow(Double temperature, Double humidity, Double wind, Double precipitation,
                      Double uvIndex, Double cloudCover, Double et, Double pressure) {}
    WeatherNow getNow(Double lat, Double lon);
}
