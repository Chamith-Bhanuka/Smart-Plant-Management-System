package lk.ijse.spring.smartplantmanagementsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "expert_profile")
@Setter
@Getter
public class ExpertProfile {
    @Id @GeneratedValue Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String about;

    @ElementCollection
    @CollectionTable(name = "expert_specializations", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "specialization")
    private Set<String> specializations = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String education;

    @Column(columnDefinition = "TEXT")
    private String experience;
}
