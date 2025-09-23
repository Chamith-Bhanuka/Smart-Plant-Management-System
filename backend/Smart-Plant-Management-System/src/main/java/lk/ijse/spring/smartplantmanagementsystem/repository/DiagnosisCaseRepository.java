package lk.ijse.spring.smartplantmanagementsystem.repository;

import lk.ijse.spring.smartplantmanagementsystem.entity.DiagnosisCase;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiagnosisCaseRepository extends JpaRepository<DiagnosisCase, Long> {
    List<DiagnosisCase> findByUserOrderByTimestampDesc(User user);
    Optional<DiagnosisCase> findByCaseCode(String caseCode);
}
