package com.testpulse.service;

import com.testpulse.dto.ReferralApplyResponse;
import com.testpulse.dto.ReferralStatusResponse;
import com.testpulse.model.Referral;
import com.testpulse.model.User;
import com.testpulse.model.UserReferralApplication;
import com.testpulse.repository.ReferralRepository;
import com.testpulse.repository.UserReferralApplicationRepository;
import com.testpulse.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class ReferralApplicationService {

    private static final String LOCKED_MESSAGE = "Referral code has already been locked for this account.";
    private static final String INVALID_CODE_MESSAGE = "Invalid Teacher/Referral Code. Please check and try again.";
    private static final String SELF_REFERRAL_MESSAGE = "You cannot use your own referral code.";

    private final UserRepository userRepository;
    private final ReferralRepository referralRepository;
    private final UserReferralApplicationRepository applicationRepository;

    public ReferralApplicationService(UserRepository userRepository,
                                      ReferralRepository referralRepository,
                                      UserReferralApplicationRepository applicationRepository) {
        this.userRepository = userRepository;
        this.referralRepository = referralRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    @CacheEvict(cacheNames = "referralStatuses", key = "#userId")
    public ReferralApplyResponse apply(Long userId, String requestedReferralId) {
        if (applicationRepository.findByUser_Id(userId).isPresent()) {
            throw new IllegalStateException(LOCKED_MESSAGE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found."));
        String referralCode = requestedReferralId == null ? "" : requestedReferralId.trim();
        Referral referral = referralRepository.findByReferralIdIgnoreCase(referralCode)
                .orElseThrow(() -> new NoSuchElementException(INVALID_CODE_MESSAGE));

        if (samePhoneNumber(user.getMobileNumber(), referral.getMobileNo())) {
            throw new IllegalArgumentException(SELF_REFERRAL_MESSAGE);
        }

        UserReferralApplication application = UserReferralApplication.builder()
                .user(user)
                .referral(referral)
                .locked(true)
                .build();
        applicationRepository.save(application);

        return ReferralApplyResponse.builder()
                .success(true)
                .message("Referral code linked successfully!")
                .isLocked(true)
                .referralId(referral.getReferralId())
                .teacherName(referral.getTeacherName())
                .build();
    }

            @Transactional(readOnly = true)
    @Cacheable(cacheNames = "referralStatuses", key = "#userId")
            public ReferralStatusResponse getStatus(Long userId) {
            return applicationRepository.findByUser_Id(userId)
                .map(application -> ReferralStatusResponse.builder()
                    .success(true)
                    .isLocked(application.isLocked())
                    .referralId(application.getReferral().getReferralId())
                    .teacherName(application.getReferral().getTeacherName())
                    .linkedAt(formatUtc(application.getCreatedTimestamp()))
                    .build())
                .orElseGet(() -> ReferralStatusResponse.builder()
                    .success(true)
                    .isLocked(false)
                    .referralId(null)
                    .teacherName(null)
                    .linkedAt(null)
                    .build());
            }

            private String formatUtc(java.time.LocalDateTime timestamp) {
            return timestamp == null ? null : DateTimeFormatter.ISO_INSTANT.format(
                timestamp.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toInstant());
            }

    private boolean samePhoneNumber(String userPhone, String teacherPhone) {
        if (userPhone == null || teacherPhone == null) {
            return false;
        }
        String normalizedUserPhone = normalizePhoneNumber(userPhone);
        String normalizedTeacherPhone = normalizePhoneNumber(teacherPhone);
        return !normalizedUserPhone.isEmpty() && normalizedUserPhone.equals(normalizedTeacherPhone);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        String digits = phoneNumber.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits.substring(2);
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return digits.substring(1);
        }
        return digits;
    }
}