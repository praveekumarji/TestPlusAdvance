package com.testpulse.service;

import com.testpulse.model.EducationClass;
import com.testpulse.model.SubscriptionStatus;
import com.testpulse.model.User;
import com.testpulse.model.UserSubscription;
import com.testpulse.repository.EducationClassRepository;
import com.testpulse.repository.UserSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserSubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final EducationClassRepository educationClassRepository;

    public UserSubscriptionService(UserSubscriptionRepository userSubscriptionRepository,
                                   EducationClassRepository educationClassRepository) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.educationClassRepository = educationClassRepository;
    }

    @Transactional
    public UserSubscription activate(User user, Long classId, String planId, String planCode,
                                    int durationDays, SubscriptionStatus status) {
        EducationClass educationClass = educationClassRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found."));
        UserSubscription subscription = userSubscriptionRepository
                .findByUser_IdAndEducationClass_Id(user.getId(), classId)
                .orElseGet(() -> UserSubscription.builder()
                        .user(user)
                        .educationClass(educationClass)
                        .build());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startsAt = subscription.getExpiresAt() != null && subscription.getExpiresAt().isAfter(now)
                ? subscription.getExpiresAt() : now;
        subscription.setStatus(status);
        subscription.setPlanId(planId);
        subscription.setPlanCode(planCode);
        subscription.setStartedAt(startsAt);
        subscription.setExpiresAt(startsAt.plusDays(durationDays));
        return userSubscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public List<Long> getPaidClassIds(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return userSubscriptionRepository.findByUser_IdAndStatus(userId, SubscriptionStatus.PAID).stream()
                .filter(subscription -> subscription.getExpiresAt() == null
                        || subscription.getExpiresAt().isAfter(now))
                .map(subscription -> subscription.getEducationClass().getId())
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional
    public User resolvePrimaryClassSubscription(User user) {
        if (user == null || userSubscriptionRepository == null) {
            return user;
        }
        if (user.getEducationClass() == null) {
            if (isActive(user.getSubscriptionStatus()) && user.getSubscriptionExpiry() != null
                    && !user.getSubscriptionExpiry().isAfter(LocalDateTime.now())) {
                user.setSubscriptionStatus(SubscriptionStatus.FREE);
            }
            return user;
        }

        Long classId = user.getEducationClass().getId();
        Optional<UserSubscription> found = userSubscriptionRepository
                .findByUser_IdAndEducationClass_Id(user.getId(), classId);
        UserSubscription subscription = found.orElseGet(() -> migrateLegacySubscription(user, classId));
        if (subscription == null) {
            user.setSubscriptionStatus(SubscriptionStatus.FREE);
            user.setSubscriptionPlan(null);
            user.setSubscriptionExpiry(null);
            user.setSubscriptionClassId(null);
            setLegacyClassStatus(user, classId, SubscriptionStatus.FREE);
            return user;
        }

        if (isActive(subscription.getStatus()) && subscription.getExpiresAt() != null
                && !subscription.getExpiresAt().isAfter(LocalDateTime.now())) {
            subscription.setStatus(SubscriptionStatus.FREE);
            userSubscriptionRepository.save(subscription);
        }
        syncLegacyFields(user, subscription);
        return user;
    }

    @Transactional
    public UserSubscription syncStatusFromLegacy(User user, SubscriptionStatus status) {
        if (user == null || user.getEducationClass() == null || userSubscriptionRepository == null) {
            return null;
        }
        Long classId = user.getEducationClass().getId();
        UserSubscription subscription = userSubscriptionRepository
                .findByUser_IdAndEducationClass_Id(user.getId(), classId)
                .orElseGet(() -> UserSubscription.builder()
                        .user(user)
                        .educationClass(user.getEducationClass())
                        .build());
        subscription.setStatus(status == null ? SubscriptionStatus.FREE : status);
        subscription.setPlanCode(user.getSubscriptionPlan());
        subscription.setExpiresAt(user.getSubscriptionExpiry());
        if (subscription.getStartedAt() == null) {
            subscription.setStartedAt(LocalDateTime.now());
        }
        return userSubscriptionRepository.save(subscription);
    }

    private UserSubscription migrateLegacySubscription(User user, Long classId) {
        SubscriptionStatus classStatus = getClassStatus(user, classId);
        SubscriptionStatus legacyStatus = classStatus;
        if (legacyStatus == null) {
            legacyStatus = user.getSubscriptionStatus();
        }
        boolean belongsToClass = classId.equals(user.getSubscriptionClassId());
        if (!belongsToClass && isActive(classStatus)) {
            belongsToClass = true;
        }
        if (!belongsToClass && user.getSubscriptionClassId() == null && isActive(legacyStatus)) {
            belongsToClass = true;
        }
        if (!belongsToClass || legacyStatus == null) {
            return null;
        }

        UserSubscription legacy = UserSubscription.builder()
                .user(user)
                .educationClass(user.getEducationClass())
                .status(legacyStatus)
                .planCode(classId.equals(user.getSubscriptionClassId()) || classStatus == SubscriptionStatus.TRIAL
                    ? user.getSubscriptionPlan() : null)
                .startedAt(user.getCreatedAt())
                .expiresAt(user.getSubscriptionExpiry())
                .build();
        return userSubscriptionRepository.save(legacy);
    }

    private SubscriptionStatus getClassStatus(User user, Long classId) {
        String status = Long.valueOf(1).equals(classId)
                ? user.getClass1SubscriptionStatus()
                : Long.valueOf(2).equals(classId) ? user.getClass2SubscriptionStatus() : null;
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return SubscriptionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void syncLegacyFields(User user, UserSubscription subscription) {
        Long classId = subscription.getEducationClass().getId();
        user.setSubscriptionStatus(subscription.getStatus());
        user.setSubscriptionPlan(subscription.getPlanCode() == null
                ? subscription.getPlanId() : subscription.getPlanCode());
        user.setSubscriptionExpiry(subscription.getExpiresAt());
        user.setSubscriptionClassId(classId);
        setLegacyClassStatus(user, classId, subscription.getStatus());
    }

    private void setLegacyClassStatus(User user, Long classId, SubscriptionStatus status) {
        if (Long.valueOf(1).equals(classId)) {
            user.setClass1SubscriptionStatus(status.name());
        } else if (Long.valueOf(2).equals(classId)) {
            user.setClass2SubscriptionStatus(status.name());
        }
    }

    private boolean isActive(SubscriptionStatus status) {
        return status == SubscriptionStatus.TRIAL || status == SubscriptionStatus.PAID
                || status == SubscriptionStatus.PRIME;
    }
}