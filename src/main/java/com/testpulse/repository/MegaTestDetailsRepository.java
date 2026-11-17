package com.testpulse.repository;

import com.testpulse.model.MegaTestDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MegaTestDetailsRepository extends JpaRepository<MegaTestDetails, Long> {
    Optional<MegaTestDetails> findByClassIdAndTestDate(Long classId, LocalDate testDate);

    Optional<MegaTestDetails> findFirstByClassIdOrderByTestDateDescIdDesc(Long classId);

    @Transactional
    void deleteByClassIdAndTestDateNot(Long classId, LocalDate testDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select details from MegaTestDetails details where details.id = :id")
    Optional<MegaTestDetails> findByIdForUpdate(@Param("id") Long id);
}
