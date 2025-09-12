package lk.ijse.spring.smartplantmanagementsystem.controller;

import lk.ijse.spring.smartplantmanagementsystem.dto.AiChatRequest;
import lk.ijse.spring.smartplantmanagementsystem.dto.AiChatResponse;
import lk.ijse.spring.smartplantmanagementsystem.repository.UserRepository;
import lk.ijse.spring.smartplantmanagementsystem.service.AiAgentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAgentController {
    private static final Logger log = LoggerFactory.getLogger(AiAgentController.class);

    private final AiAgentService aiService;
    private final UserRepository userRepo;

    /**
     * Handle an AI query: generate SQL, execute it, summarize the result.
     * Uses DeferredResult to extend async timeout and give a friendly fallback.
     */
    @PostMapping("/query")
    public DeferredResult<ResponseEntity<AiChatResponse>> query(
            @RequestBody AiChatRequest req,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        DeferredResult<ResponseEntity<AiChatResponse>> deferred = new DeferredResult<>(600_000L);
        deferred.onTimeout(() -> {
            log.warn("AI request timed out after 600 seconds");
            AiChatResponse timeoutResponse = new AiChatResponse(
                    null,
                    java.util.List.of(),
                    "Sorry, this is taking longer than expected. Please try again later."
            );
            deferred.setResult(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(timeoutResponse));
        });

        if (userDetails == null) {
            log.warn("Unauthorized AI request");
            deferred.setResult(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            return deferred;
        }

        // Look up the actual numeric user ID
        CompletableFuture
                .supplyAsync(() -> userRepo.findByEmail(userDetails.getUsername())
                        .orElseThrow(() -> new IllegalStateException("User not found"))
                        .getId())
                .thenCompose(userId ->
                        aiService.handleAsync(
                                req.getQuestion(),
                                req.getPlantId(),
                                req.isSqlOnly(),
                                userId
                        )
                )
                .whenComplete((aiResponse, ex) -> {
                    if (ex != null) {
                        log.error("Error processing AI request", ex);
                        AiChatResponse errorResp = new AiChatResponse(
                                null,
                                java.util.List.of(),
                                "An unexpected error occurred. Please try again."
                        );
                        deferred.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResp));
                    } else {
                        log.info("AI response ready, sending to client");
                        deferred.setResult(ResponseEntity.ok(aiResponse));
                    }
                });

        return deferred;
    }
}