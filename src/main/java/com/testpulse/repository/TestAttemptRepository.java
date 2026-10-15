package com.testpulse.repository;

import com.testpulse.model.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
	List<TestAttempt> findByUserId(String userId);
}
