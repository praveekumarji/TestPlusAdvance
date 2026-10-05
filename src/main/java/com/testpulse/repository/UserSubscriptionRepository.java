package com.testpulse.repository;

import com.testpulse.model.UserSubscription;
import com.testpulse.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    Optional<UserSubscription> findByUser_IdAndEducationClass_Id(Long userId, Long classId);
    List<UserSubscription> findByUser_IdAndStatus(Long userId, SubscriptionStatus status);
}