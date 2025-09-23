package lk.ijse.spring.smartplantmanagementsystem.controller;

import lk.ijse.spring.smartplantmanagementsystem.dto.AssignedCaseDTO;
import lk.ijse.spring.smartplantmanagementsystem.entity.CaseAssignment;
import lk.ijse.spring.smartplantmanagementsystem.entity.CaseFeedback;
import lk.ijse.spring.smartplantmanagementsystem.entity.DiagnosisCase;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import lk.ijse.spring.smartplantmanagementsystem.repository.CaseAssignmentRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.CaseFeedbackRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.ExpertProfileRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expert")
@RequiredArgsConstructor
public class ExpertCaseController {
    private final CaseAssignmentRepository assignRepo;
    private final CaseFeedbackRepository feedbackRepo;
    private final UserRepository userRepo;
    private final JavaMailSender mailSender;
    private final ExpertProfileRepository profileRepo;

    @GetMapping("/cases")
    @PreAuthorize("hasRole('EXPERT')")
    public List<AssignedCaseDTO> myCases(@AuthenticationPrincipal UserDetails ud) {
        User expert = userRepo.findByEmail(ud.getUsername()).orElseThrow();
        return assignRepo.findByExpertOrderByAssignedAtDesc(expert).stream().map(ca -> {
            DiagnosisCase dc = ca.getDiagnosisCase();
            return new AssignedCaseDTO(
                    ca.getId(),
                    dc.getCaseCode(),
                    dc.getFilename(),
                    dc.getLabel(),
                    dc.getConfidence(),
                    dc.getInsights(),
                    ca.getAssignedAt(),
                    feedbackRepo.findByAssignment(ca).map(f -> f.getFeedback()).orElse(null)
            );
        }).toList();
    }

//    @PostMapping("/cases/{assignmentId}/feedback")
//    public void submitFeedback(
//            @PathVariable String caseCode,
//            @RequestBody Map<String,String> body,
//            @AuthenticationPrincipal UserDetails ud
//    ) {
//        CaseAssignment ca = assignRepo.findAllByDiagnosisCase_CaseCode(caseCode)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
//
//        String fb = body.get("feedback");
//        CaseFeedback cf = new CaseFeedback();
//        cf.setAssignment(ca);
//        cf.setFeedback(fb);
//        feedbackRepo.save(cf);
//
//        // email user
////        DiagnosisCase dc = ca.getDiagnosisCase();
////        String to = dc.getUser().getEmail();
////        String subject = "Expert Feedback for Case " + dc.getCaseCode();
////        String text = "Hello,\n\nYour case " + dc.getCaseCode() +
////                " was reviewed by " + ud.getUsername() +
////                ".\n\nFeedback:\n" + fb;
////
////        mailSender.send(new SimpleMailMessage() {{
////            setTo(to);
////            setSubject(subject);
////            setText(text);
////        }});
//    }
//}

    @PostMapping("/cases/{caseCode}/feedback")
    @PreAuthorize("hasRole('EXPERT')")
    public void submitFeedback(
            @PathVariable String caseCode,
            @RequestBody Map<String,String> body,
            @AuthenticationPrincipal UserDetails ud
    ) {
        CaseAssignment ca = assignRepo.findByDiagnosisCase_CaseCode(caseCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));

        String fb = body.get("feedback");
        CaseFeedback cf = new CaseFeedback();
        cf.setAssignment(ca);
        cf.setFeedback(fb);
        feedbackRepo.save(cf);

        // email logic if you want to re‑enable it
    }
}