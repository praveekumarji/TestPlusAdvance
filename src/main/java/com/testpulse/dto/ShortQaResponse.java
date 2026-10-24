package com.testpulse.dto;

import java.util.List;

public record ShortQaResponse(
        Long id,
        String subject,
        int totalQuestions,
        Long classId,
        int totalTopics,
        List<String> topics
) {
}