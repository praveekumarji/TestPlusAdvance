package com.testpulse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTestRequest {
    @NotBlank
    private String title;

    private String titleHi;

    @NotBlank
    private String subject;

    private String subjectHi;

    private String group;

    private String groupId;

    private String groupIcon;

    private Integer groupOrder;

    @NotBlank
    private String description;

    private String descriptionHi;

    private String durationMinutes;

    @NotBlank
    private String mode;

    @NotBlank
    private String difficulty;

    private Long classId;

    @Builder.Default
    private String testType = "FREE";

    private Long totalQuestions;
}
