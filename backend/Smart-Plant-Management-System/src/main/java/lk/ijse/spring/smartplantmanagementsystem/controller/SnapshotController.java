package lk.ijse.spring.smartplantmanagementsystem.controller;

import lk.ijse.spring.smartplantmanagementsystem.dto.PlantMonitorDTO;
import lk.ijse.spring.smartplantmanagementsystem.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/plants")
@RequiredArgsConstructor
public class SnapshotController {
    private final SnapshotService snapshotService;

    @PostMapping("/{plantId}/snapshot")
    public ResponseEntity<PlantMonitorDTO> snapshot(
            @PathVariable Long plantId,
            @AuthenticationPrincipal UserDetails user) {
        PlantMonitorDTO dto = snapshotService.captureLatestSnapshot(plantId, user.getUsername());
        return ResponseEntity.ok(dto);
    }
}
