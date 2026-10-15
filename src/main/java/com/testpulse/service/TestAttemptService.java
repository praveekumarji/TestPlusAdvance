package com.testpulse.service;

import com.testpulse.dto.SubmitTestAttemptRequest;
import com.testpulse.model.TestAttempt;
import java.util.List;

public interface TestAttemptService {
    TestAttempt submitTestAttempt(SubmitTestAttemptRequest request, Long userId);
    List<TestAttempt> getAllAttemptsByUserId(Long userId);
    TestAttempt getAttemptById(Long id);
}
