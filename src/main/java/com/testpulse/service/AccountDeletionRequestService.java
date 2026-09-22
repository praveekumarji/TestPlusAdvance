package com.testpulse.service;

import com.testpulse.dto.AccountDeletionRequestDto;
import com.testpulse.model.AccountDeletionRequest;
import com.testpulse.repository.AccountDeletionRequestRepository;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDeletionRequestService {

    private final AccountDeletionRequestRepository requestRepository;

    public AccountDeletionRequestService(AccountDeletionRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Transactional
    public void submit(AccountDeletionRequestDto request) {
        String email = normalize(request.getEmail());
        if (requestRepository.existsByEmailAndStatusIn(email,
                EnumSet.of(AccountDeletionRequest.Status.PENDING))) {
            return;
        }

        requestRepository.save(AccountDeletionRequest.builder()
                .email(email)
                .status(AccountDeletionRequest.Status.PENDING)
                .requestedAt(LocalDateTime.now())
                .build());
    }

    public List<AccountDeletionRequest> findAll() {
        return requestRepository.findAllByOrderByRequestedAtAsc();
    }

    @Transactional
    public AccountDeletionRequest updateStatus(Long id, AccountDeletionRequest.Status status) {
        AccountDeletionRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Deletion request not found."));
        request.setStatus(status);
        request.setProcessedAt(status == AccountDeletionRequest.Status.PENDING ? null : LocalDateTime.now());
        return requestRepository.save(request);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
