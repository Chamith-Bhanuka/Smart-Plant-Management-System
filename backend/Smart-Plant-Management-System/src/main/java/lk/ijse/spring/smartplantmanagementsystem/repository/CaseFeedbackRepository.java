package lk.ijse.spring.smartplantmanagementsystem.repository;

import lk.ijse.spring.smartplantmanagementsystem.entity.CaseAssignment;
import lk.ijse.spring.smartplantmanagementsystem.entity.CaseFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaseFeedbackRepository extends JpaRepository<CaseFeedback, Long> {
    Optional<CaseFeedback> findByAssignment(CaseAssignment assignment);
}
