package com.testpulse.dto;

import java.util.List;

public record ShortQaQuestionRequest(
        Long shortQAId,
        String subjectEn,
        String subjectHi,
        String topicEn,
        String topicHi,
        String questionEn,
        String questionHi,
        String answerEn,
        String answerHi,
        String rememberTrick,
        String rememberTrickHi,
        String type,
        Boolean active,
        List<Integer> appearYears
) {
}