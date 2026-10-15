package com.testpulse.repository;

import com.testpulse.model.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {
    Optional<Referral> findByReferralIdIgnoreCase(String referralId);

    boolean existsByReferralId(String referralId);

    boolean existsByReferralIdAndIdNot(String referralId, Long id);
}