package lk.ijse.spring.smartplantmanagementsystem.dto;

import java.time.LocalDateTime;

public record AssignedCaseDTO(
        Long assignmentId,
        String caseCode,
        String imageUrl,
        String label,
        double confidence,
        String insights,
        LocalDateTime assignedAt,
        String feedback
) {}
