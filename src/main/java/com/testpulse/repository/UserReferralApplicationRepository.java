package com.testpulse.repository;

import com.testpulse.model.UserReferralApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserReferralApplicationRepository extends JpaRepository<UserReferralApplication, Long> {
    Optional<UserReferralApplication> findByUser_Id(Long userId);
}