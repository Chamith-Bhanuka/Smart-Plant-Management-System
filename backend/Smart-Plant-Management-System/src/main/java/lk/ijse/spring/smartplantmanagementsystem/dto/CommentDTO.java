package lk.ijse.spring.smartplantmanagementsystem.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDTO {
    private Long id;
    private String authorName;
    private String authorEmail;
    private String text;
    private LocalDateTime createdAt;
}
