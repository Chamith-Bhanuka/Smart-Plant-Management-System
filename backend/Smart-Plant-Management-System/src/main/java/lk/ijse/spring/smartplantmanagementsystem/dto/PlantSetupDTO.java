package lk.ijse.spring.smartplantmanagementsystem.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PlantSetupDTO {
    private MultipartFile image;
    private Double latitude;
    private Double longitude;
    private Long optimalConditionsId;
}
