package com.testpulse.service;

import com.testpulse.dto.ReferralRequest;
import com.testpulse.model.Referral;
import com.testpulse.repository.ReferralRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReferralService {

    private final ReferralRepository referralRepository;

    public ReferralService(ReferralRepository referralRepository) {
        this.referralRepository = referralRepository;
    }

    @Transactional(readOnly = true)
    public List<Referral> getAll() {
        return referralRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Referral getById(Long id) {
        return referralRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Referral not found."));
    }

    @Transactional
    public Referral create(ReferralRequest request) {
        String referralId = request.getReferralId().trim();
        if (referralRepository.existsByReferralId(referralId)) {
            throw new IllegalArgumentException("Referral ID already exists.");
        }
        return referralRepository.save(Referral.builder()
                .referralId(referralId)
                .teacherName(request.getTeacherName().trim())
                .agentCode(normalizeOptional(request.getAgentCode()))
                .agentName(normalizeOptional(request.getAgentName()))
                .agentMobile(normalizeOptional(request.getAgentMobile()))
                .mobileNo(request.getMobileNo().trim())
                .address(normalizeAddress(request.getAddress()))
                .build());
    }

    @Transactional
    @CacheEvict(cacheNames = "referralStatuses", allEntries = true)
    public Referral update(Long id, ReferralRequest request) {
        Referral referral = referralRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Referral not found."));
        String referralId = request.getReferralId().trim();
        if (referralRepository.existsByReferralIdAndIdNot(referralId, id)) {
            throw new IllegalArgumentException("Referral ID already exists.");
        }
        referral.setReferralId(referralId);
        referral.setTeacherName(request.getTeacherName().trim());
        referral.setAgentCode(normalizeOptional(request.getAgentCode()));
        referral.setAgentName(normalizeOptional(request.getAgentName()));
        referral.setAgentMobile(normalizeOptional(request.getAgentMobile()));
        referral.setMobileNo(request.getMobileNo().trim());
        referral.setAddress(normalizeAddress(request.getAddress()));
        return referralRepository.save(referral);
    }

    @Transactional
    @CacheEvict(cacheNames = "referralStatuses", allEntries = true)
    public void delete(Long id) {
        Referral referral = referralRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Referral not found."));
        referralRepository.delete(referral);
    }

    private String normalizeAddress(String address) {
        return address == null || address.isBlank() ? null : address.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}