package lk.ijse.spring.smartplantmanagementsystem.service;

import org.springframework.stereotype.Service;

@Service
public class HttpSensorProvider implements SensorProvider {
    @Override
    public SensorNow getNow(Long plantId) {
        // GET http://sensor-gateway/sensors/latest?plantId=...
        // parse JSON
        return new SensorNow(27.0, 82.0, 55.0, 900.0);
    }
}
