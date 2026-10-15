package com.testpulse.controller;

import com.testpulse.config.JwtAuthenticationToken;
import com.testpulse.dto.SubmitTestAttemptRequest;
import com.testpulse.model.TestAttempt;
import com.testpulse.service.TestAttemptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attempts")
public class TestAttemptController {

    private final TestAttemptService testAttemptService;

    public TestAttemptController(TestAttemptService testAttemptService) {
        this.testAttemptService = testAttemptService;
    }

    @PostMapping
    public ResponseEntity<?> submitAttempt(@Valid @RequestBody SubmitTestAttemptRequest request) {
        try {
            Long userId = getAuthenticatedUserId();
            TestAttempt savedAttempt = testAttemptService.submitTestAttempt(request, userId);
            return ResponseEntity.ok(savedAttempt);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        }
    }

    private Long getAuthenticatedUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("A valid JWT is required.");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new AccessDeniedException("JWT does not contain a valid user ID.");
        }
    }

    @GetMapping
    public ResponseEntity<List<TestAttempt>> getAllAttempts(@RequestParam Long userId) {
        List<TestAttempt> attempts = testAttemptService.getAllAttemptsByUserId(userId);
        return ResponseEntity.ok(attempts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestAttempt> getAttemptById(@PathVariable Long id) {
        TestAttempt attempt = testAttemptService.getAttemptById(id);
        return ResponseEntity.ok(attempt);
    }
}
