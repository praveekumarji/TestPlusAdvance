package com.testpulse.service.impl;

import com.testpulse.dto.SubmitTestAttemptRequest;
import com.testpulse.model.TestAttempt;
import com.testpulse.model.Test;
import com.testpulse.repository.TestAttemptRepository;
import com.testpulse.repository.TestRepository;
import com.testpulse.service.TestAttemptService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class TestAttemptServiceImpl implements TestAttemptService {

    private final TestAttemptRepository testAttemptRepository;
    private final TestRepository testRepository;

    public TestAttemptServiceImpl(TestAttemptRepository testAttemptRepository, TestRepository testRepository) {
        this.testAttemptRepository = testAttemptRepository;
        this.testRepository = testRepository;
    }

    @Override
    @CacheEvict(value = "testAttempts", allEntries = true)
    @Transactional
    public TestAttempt submitTestAttempt(SubmitTestAttemptRequest request, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Authenticated user ID is required.");
        }
        if (request.getCorrectCount() > request.getTotalQuestions()) {
            throw new IllegalArgumentException("Correct count cannot exceed total questions.");
        }

        Long testId = parseTestId(request.getTestId());
        Test test = testRepository.findById(testId)
                .filter(Test::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Test not found."));
        if (test.getEducationClass() == null
            || !test.getEducationClass().getId().equals(request.getClassId().longValue())) {
            throw new IllegalArgumentException("Class ID does not match the selected test.");
        }

        TestAttempt testAttempt = TestAttempt.builder()
            .userId(userId.toString())
            .classId(request.getClassId())
            .testId(request.getTestId().trim())
                .scorePercentage(request.getScorePercentage())
                .correctCount(request.getCorrectCount())
                .totalQuestions(request.getTotalQuestions())
                .timeTakenSeconds(request.getTimeTakenSeconds())
            .createdAt(LocalDateTime.now())
                .build();
        return testAttemptRepository.save(testAttempt);
    }

    private Long parseTestId(String requestTestId) {
        String normalizedId = requestTestId.trim();
        if (normalizedId.toLowerCase(Locale.ROOT).startsWith("test_")) {
            normalizedId = normalizedId.substring("test_".length());
        }
        try {
            long testId = Long.parseLong(normalizedId);
            if (testId <= 0) {
                throw new NumberFormatException("Test ID must be positive.");
            }
            return testId;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("testId must be a numeric ID or use the test_<id> format.");
        }
    }

    @Override
    @Cacheable(value = "testAttempts", key = "'user:' + #userId")
    public List<TestAttempt> getAllAttemptsByUserId(Long userId) {
        return testAttemptRepository.findByUserId(userId.toString());
    }

    @Override
    @Cacheable(value = "testAttempts", key = "'id:' + #id")
    public TestAttempt getAttemptById(Long id) {
        return testAttemptRepository.findById(id).orElseThrow(() -> new RuntimeException("Attempt not found"));
    }
}
