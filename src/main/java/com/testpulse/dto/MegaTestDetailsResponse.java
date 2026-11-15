package com.testpulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MegaTestDetailsResponse {
    private String title;
    private String subject;
    private String description;
    private int durationMinutes;
    private int totalQuestions;
    private String mode;
    private String testType;
    private List<CustomQuestionResponse> questions;
}
