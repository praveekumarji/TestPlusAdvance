package com.testpulse.controller;

import com.testpulse.config.JwtAuthenticationToken;
import com.testpulse.dto.ReferralApplyRequest;
import com.testpulse.dto.ReferralApplyResponse;
import com.testpulse.dto.ReferralStatusResponse;
import com.testpulse.service.ReferralApplicationService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/referral")
public class ReferralApplicationController {

    private static final String LOCKED_MESSAGE = "Referral code has already been locked for this account.";
    private static final String SELF_REFERRAL_MESSAGE = "You cannot use your own referral code.";

    private final ReferralApplicationService referralApplicationService;

    public ReferralApplicationController(ReferralApplicationService referralApplicationService) {
        this.referralApplicationService = referralApplicationService;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam(name = "user_id", required = false) String requestUserId) {
        try {
            Long authenticatedUserId = getAuthenticatedUserId();
            if (requestUserId != null && !matchesAuthenticatedUser(requestUserId, authenticatedUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "User ID does not match the authenticated user."));
            }
            ReferralStatusResponse response = referralApplicationService.getStatus(authenticatedUserId);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<?> apply(@Valid @RequestBody ReferralApplyRequest request) {
        try {
            Long authenticatedUserId = getAuthenticatedUserId();
            if (!matchesAuthenticatedUser(request.getUserId(), authenticatedUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "User ID does not match the authenticated user."));
            }
            ReferralApplyResponse response = referralApplicationService.apply(
                    authenticatedUserId, request.getReferralId());
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", LOCKED_MESSAGE));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", LOCKED_MESSAGE));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", SELF_REFERRAL_MESSAGE));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", ex.getMessage()));
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

    private boolean matchesAuthenticatedUser(String requestUserId, Long authenticatedUserId) {
        String normalizedRequestId = requestUserId.trim();
        if (normalizedRequestId.regionMatches(true, 0, "usr_", 0, 4)) {
            normalizedRequestId = normalizedRequestId.substring(4);
        }
        return normalizedRequestId.equals(authenticatedUserId.toString());
    }
}