package com.testpulse.dto;

import java.util.List;

public record ShortQaQuestionResponse(
        Long id,
        String topic,
        String question,
        String answer,
        String rememberTrick,
        String type,
        boolean active,
        List<Integer> appearYears
) {
}