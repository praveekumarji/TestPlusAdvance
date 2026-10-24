package com.testpulse.dto;

import java.util.List;

public record ShortQaQuestionGroupResponse(
        Long shortQAId,
        String subject,
        List<ShortQaQuestionResponse> questions
) {
}