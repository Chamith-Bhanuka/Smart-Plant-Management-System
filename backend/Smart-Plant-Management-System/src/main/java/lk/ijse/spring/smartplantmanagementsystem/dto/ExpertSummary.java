package lk.ijse.spring.smartplantmanagementsystem.dto;

import java.util.Set;

public record ExpertSummary(
        Long id,
        String email,
        String about,
        Set<String> specializations,
        String education,
        String experience
) {}
