package com.testpulse.controller;

import com.testpulse.dto.AccountDeletionRequestDto;
import com.testpulse.dto.AccountDeletionStatusDto;
import com.testpulse.model.AccountDeletionRequest;
import com.testpulse.service.AccountDeletionRequestService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account-deletion")
public class AccountDeletionController {

    private final AccountDeletionRequestService requestService;

    public AccountDeletionController(AccountDeletionRequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping("/requests")
    public ResponseEntity<Map<String, String>> submit(@Valid @RequestBody AccountDeletionRequestDto request) {
        requestService.submit(request);
        return ResponseEntity.accepted().body(Map.of(
                "message", "Your deletion request has been received. We will process it within 30 days."
        ));
    }

    @GetMapping("/requests")
    public List<AccountDeletionRequest> findAll() {
        return requestService.findAll();
    }

    @PatchMapping("/requests/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                           @Valid @RequestBody AccountDeletionStatusDto request) {
        try {
            return ResponseEntity.ok(requestService.updateStatus(id, request.getStatus()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
