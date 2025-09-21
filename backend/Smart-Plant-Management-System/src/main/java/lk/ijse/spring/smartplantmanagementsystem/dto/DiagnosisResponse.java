package lk.ijse.spring.smartplantmanagementsystem.dto;

import java.time.LocalDateTime;

public record DiagnosisResponse(
        String caseCode,
        String imageUrl,
        String label,
        double confidence,
        String insights,
        LocalDateTime timestamp
) {}
