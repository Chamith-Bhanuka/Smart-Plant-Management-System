package lk.ijse.spring.smartplantmanagementsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_feedback")
@Getter
@Setter
public class CaseFeedback {
    @Id
    @GeneratedValue
    Long id;

    @OneToOne
    @JoinColumn(name = "assignment_id", nullable = false)
    private CaseAssignment assignment;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(nullable = false)
    private LocalDateTime providedAt = LocalDateTime.now();
}
