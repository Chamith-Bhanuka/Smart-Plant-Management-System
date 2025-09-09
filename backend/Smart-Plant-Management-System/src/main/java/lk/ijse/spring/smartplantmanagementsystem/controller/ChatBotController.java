package lk.ijse.spring.smartplantmanagementsystem.controller;

import lk.ijse.spring.smartplantmanagementsystem.dto.PlantChatDTO;
import lk.ijse.spring.smartplantmanagementsystem.service.PlantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public class ChatBotController {
    private final PlantService plantService;

    public ChatBotController(PlantService plantService) {
        this.plantService = plantService;
    }

    @GetMapping("/plant")
    public ResponseEntity<PlantChatDTO> getPlantData(@RequestParam String name, @AuthenticationPrincipal UserDetails user) {
        PlantChatDTO dto = plantService.getPlantChatData(name, user.getUsername());
        return ResponseEntity.ok(dto);
    }
}
