package lk.ijse.spring.smartplantmanagementsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "diagnosis_case")
@Getter
@Setter
public class DiagnosisCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String caseCode;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private double confidence;

    @Column(columnDefinition = "TEXT")
    private String insights;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
}
