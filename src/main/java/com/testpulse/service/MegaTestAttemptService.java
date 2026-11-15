package com.testpulse.service;

import com.testpulse.dto.MegaTestAttemptResponse;
import com.testpulse.dto.MegaTestLeaderboardResponse;
import com.testpulse.dto.SubmitMegaTestAttemptRequest;

public interface MegaTestAttemptService {
    MegaTestAttemptResponse submitAttempt(SubmitMegaTestAttemptRequest request, Long authenticatedUserId);

    MegaTestLeaderboardResponse getLeaderboard(String testId, Integer classId, String userId);

    MegaTestLeaderboardResponse getCurrentLeaderboard(Long classId, String userId);
}
