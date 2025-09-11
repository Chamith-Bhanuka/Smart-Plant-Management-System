package lk.ijse.spring.smartplantmanagementsystem.controller;

import lk.ijse.spring.smartplantmanagementsystem.dto.AiChatRequest;
import lk.ijse.spring.smartplantmanagementsystem.dto.AiChatResponse;
import lk.ijse.spring.smartplantmanagementsystem.service.AiAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAgentController {
    private final AiAgentService aiAgentService;

    @PostMapping("/query")
    public ResponseEntity<AiChatResponse> query(@RequestBody AiChatRequest req,
                                                @AuthenticationPrincipal UserDetails user) throws Exception{
        AiChatResponse resp = aiAgentService.handle(req.getQuestion(), req.getPlantId(), req.isSqlOnly());
        return ResponseEntity.ok(resp);
    }
}
