package lk.ijse.spring.smartplantmanagementsystem.repository;

import lk.ijse.spring.smartplantmanagementsystem.entity.CaseAssignment;
import lk.ijse.spring.smartplantmanagementsystem.entity.DiagnosisCase;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaseAssignmentRepository extends JpaRepository<CaseAssignment, Long> {
    List<CaseAssignment> findByExpertOrderByAssignedAtDesc(User expert);
    boolean existsByDiagnosisCase(DiagnosisCase dc);
    Optional<CaseAssignment> findByDiagnosisCase_CaseCode(String caseCode);

}
