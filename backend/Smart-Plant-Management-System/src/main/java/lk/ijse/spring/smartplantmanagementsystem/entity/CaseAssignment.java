package lk.ijse.spring.smartplantmanagementsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_assignment")
@Getter
@Setter
public class CaseAssignment {
    @Id
    @GeneratedValue
    Long id;

    @ManyToOne
    @JoinColumn(name = "expert_id", nullable = false)
    private User expert;

    @ManyToOne @JoinColumn(name = "case_id", nullable = false)
    private DiagnosisCase diagnosisCase;

    @Column(nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();
}
