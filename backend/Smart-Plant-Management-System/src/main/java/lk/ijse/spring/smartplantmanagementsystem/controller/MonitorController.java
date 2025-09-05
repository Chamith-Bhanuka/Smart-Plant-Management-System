package lk.ijse.spring.smartplantmanagementsystem.controller;

import lk.ijse.spring.smartplantmanagementsystem.dto.PlantMonitorDTO;
import lk.ijse.spring.smartplantmanagementsystem.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/plants")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @GetMapping("/monitor")
    public ResponseEntity<List<PlantMonitorDTO>> getMonitor(@AuthenticationPrincipal UserDetails userDetails) {
        List<PlantMonitorDTO> data = monitorService.getMonitorData(userDetails.getUsername());
        return ResponseEntity.ok(data);
    }
}
