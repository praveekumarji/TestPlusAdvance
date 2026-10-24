package com.testpulse.dto;

import java.util.List;

public record ShortQaRequest(
        String subject,
        String subjectHi,
        int totalQuestions,
        Long classId,
        int totalTopics,
        List<String> topics,
        List<String> topicsHi
) {
}