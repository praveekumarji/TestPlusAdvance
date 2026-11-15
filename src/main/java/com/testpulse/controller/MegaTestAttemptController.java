package com.testpulse.controller;

import com.testpulse.config.JwtAuthenticationToken;
import com.testpulse.dto.MegaTestAttemptResponse;
import com.testpulse.dto.MegaTestLeaderboardResponse;
import com.testpulse.dto.SubmitMegaTestAttemptRequest;
import com.testpulse.service.MegaTestAttemptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mega-test")
public class MegaTestAttemptController {

    private final MegaTestAttemptService attemptService;

    public MegaTestAttemptController(MegaTestAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitAttempt(@Valid @RequestBody SubmitMegaTestAttemptRequest request,
                                           HttpServletRequest servletRequest) {
        try {
            Long authenticatedUserId = getAuthenticatedUserIdIfProvided(servletRequest);
            MegaTestAttemptResponse response = attemptService.submitAttempt(request, authenticatedUserId);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{testId}/leaderboard")
    public ResponseEntity<?> getLeaderboard(@PathVariable String testId,
                                            @RequestParam(required = false) Integer classId,
                                            @RequestParam(required = false) String userId) {
        try {
            MegaTestLeaderboardResponse response =
                    attemptService.getLeaderboard(testId, classId, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> getCurrentLeaderboard(@RequestParam Long classId,
                                                   @RequestParam(required = false) String userId) {
        try {
            MegaTestLeaderboardResponse response =
                    attemptService.getCurrentLeaderboard(classId, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private Long getAuthenticatedUserIdIfProvided(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("The supplied bearer token is invalid.");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new AccessDeniedException("JWT does not contain a valid user ID.");
        }
    }
}
