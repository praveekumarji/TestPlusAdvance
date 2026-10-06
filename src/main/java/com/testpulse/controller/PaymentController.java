package com.testpulse.controller;

import com.testpulse.dto.PaymentFailureRequest;
import com.testpulse.dto.PaymentInitiateRequest;
import com.testpulse.dto.PaymentRecordRequest;
import com.testpulse.dto.PaymentSuccessRequest;
import com.testpulse.model.Payment;
import com.testpulse.model.PaymentStatus;
import com.testpulse.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody PaymentInitiateRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Payment payload is required.");
            }
            Long authenticatedUserId = getAuthenticatedUserId();
            if (authenticatedUserId == null || !authenticatedUserId.equals(request.getUserId())) {
                return ResponseEntity.status(403).body(Map.of("error", "User ID does not match the authenticated user."));
            }

            Payment payment = paymentService.initiatePayment(
                    authenticatedUserId,
                    request.getPlanId(),
                    request.getPlanName(),
                    request.getAmount()
            );

            return ResponseEntity.ok(Map.of(
                    "paymentId", payment.getId(),
                    "userId", payment.getUser() != null ? payment.getUser().getId() : null,
                    "planName", payment.getPlanName(),
                    "amount", payment.getAmount(),
                    "status", payment.getStatus().name(),
                    "createdAt", payment.getCreatedAt()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping({"/record-success", "/records-success"})
    public ResponseEntity<?> recordSuccess(@Valid @RequestBody PaymentRecordRequest request) {
        return recordPaymentForAuthenticatedUser(request);
    }

    @PostMapping("/record-failure")
    public ResponseEntity<?> recordFailure(@RequestBody PaymentFailureRequest request) {
        try {
            Payment payment = paymentService.recordFailure(request.getPaymentId(), request.getReason());
            return ResponseEntity.ok(Map.of(
                    "paymentId", payment.getId(),
                    "status", payment.getStatus().name(),
                    "failureReason", payment.getFailureReason()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/records")
    public ResponseEntity<?> recordPayment(@Valid @RequestBody PaymentRecordRequest request) {
        return recordPaymentForAuthenticatedUser(request);
    }

    private ResponseEntity<?> recordPaymentForAuthenticatedUser(PaymentRecordRequest request) {
        Long authenticatedUserId = getAuthenticatedUserId();
        if (authenticatedUserId == null || !authenticatedUserId.equals(request.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("error", "User ID does not match the authenticated user."));
        }
        try {
            log.info("Recording payment :{}", request.toString());
            return ResponseEntity.ok(paymentService.recordPayment(request, authenticatedUserId));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
