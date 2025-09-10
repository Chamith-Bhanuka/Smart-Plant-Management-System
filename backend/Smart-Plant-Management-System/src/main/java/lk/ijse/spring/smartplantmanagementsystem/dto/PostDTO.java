package lk.ijse.spring.smartplantmanagementsystem.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private String coverImageUrl;
    private List<String> tags;
    private int upVotes;
    private int downVotes;
    private String authorName;
    private String authorEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String userVote;
}
