package lk.ijse.spring.smartplantmanagementsystem.service;

public interface SensorProvider {
    record SensorNow(Double airTemperature, Double airHumidity, Double soilMoisture, Double lightIntensity) {}
    SensorNow getNow(Long plantId);
}
