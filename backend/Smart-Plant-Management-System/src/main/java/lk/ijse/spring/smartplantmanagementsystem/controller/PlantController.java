package lk.ijse.spring.smartplantmanagementsystem.controller;

import lk.ijse.spring.smartplantmanagementsystem.entity.Plant;
import lk.ijse.spring.smartplantmanagementsystem.service.PlantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/plants")
@RequiredArgsConstructor
public class PlantController {

    private final PlantService plantService;

    @PostMapping("/upload")
    public Plant uploadAndIdentify(@RequestParam("file") MultipartFile file) throws Exception{
        return plantService.identifyAndSave(file);
    }
}
