package lk.ijse.spring.smartplantmanagementsystem.dto;

import lombok.Data;

@Data
public class AiChatRequest {
    private String question;
    private Long plantId;
    private boolean sqlOnly;
}
