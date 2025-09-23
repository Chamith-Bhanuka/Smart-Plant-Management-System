package lk.ijse.spring.smartplantmanagementsystem.controller;

import lk.ijse.spring.smartplantmanagementsystem.dto.ExpertSummary;
import lk.ijse.spring.smartplantmanagementsystem.entity.CaseAssignment;
import lk.ijse.spring.smartplantmanagementsystem.entity.DiagnosisCase;
import lk.ijse.spring.smartplantmanagementsystem.entity.ExpertProfile;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import lk.ijse.spring.smartplantmanagementsystem.repository.CaseAssignmentRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.DiagnosisCaseRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.ExpertProfileRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/experts")
@RequiredArgsConstructor
public class ExpertController {
    private final UserRepository userRepo;
    private final ExpertProfileRepository profileRepo;
    private final CaseAssignmentRepository assignRepo;
    private final DiagnosisCaseRepository caseRepo;

//    @GetMapping
//    public List<ExpertSummary> listExperts() {
//        return profileRepo.findAll().stream().map(p ->
//                new ExpertSummary(
//                        p.getUser().getId(),
//                        p.getUser().getEmail(),
//                        p.getAbout(),
//                        p.getSpecializations(),
//                        p.getEducation(),
//                        p.getExperience()
//                )
//        ).toList();
//    }

//    @PostMapping("/{expertId}/assign-case")
//    public void assignCase(
//            @PathVariable Long expertId,
//            @RequestBody Map<String,String> body
//    ) {
//        String code = body.get("caseCode");
//        DiagnosisCase dc = caseRepo.findByCaseCode(code)
//                .orElseThrow();
//        User expert = userRepo.findById(expertId).orElseThrow();
//        CaseAssignment ca = new CaseAssignment();
//        ca.setExpert(expert);
//        ca.setDiagnosisCase(dc);
//        assignRepo.save(ca);
//    }

    @PostMapping("/{expertId}/assign-case")
    public ResponseEntity<?> assignCase(
            @PathVariable Long expertId,
            @RequestBody Map<String,String> body
    ) {
        String code = body.get("caseCode");
        DiagnosisCase dc = caseRepo.findByCaseCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));

        boolean alreadyAssigned = assignRepo.existsByDiagnosisCase(dc);
        if (alreadyAssigned) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Case already assigned to an expert");
        }

        User expert = userRepo.findById(expertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expert not found"));

        CaseAssignment ca = new CaseAssignment();
        ca.setExpert(expert);
        ca.setDiagnosisCase(dc);
        assignRepo.save(ca);

        return ResponseEntity.ok().build();
    }


    @GetMapping
    public List<ExpertSummary> listExperts(@RequestParam(value = "q", required = false) String q) {
        List<ExpertProfile> profiles;
        if (q == null || q.isBlank()) {
            profiles = profileRepo.findAll();
        } else {
            profiles = profileRepo.searchAll(q.toLowerCase());
        }
        return profiles.stream().map(p ->
                new ExpertSummary(
                        p.getUser().getId(),
                        p.getUser().getEmail(),
                        p.getAbout(),
                        p.getSpecializations(),
                        p.getEducation(),
                        p.getExperience()
                )
        ).toList();
    }

}
