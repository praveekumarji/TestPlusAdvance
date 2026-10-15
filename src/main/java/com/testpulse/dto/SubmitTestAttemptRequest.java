package com.testpulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class SubmitTestAttemptRequest {

    @NotBlank
    private String testId;

    @NotBlank
    private String testTitle;

    @NotNull
    @Positive
    private Integer classId;

    @NotBlank
    private String subject;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer scorePercentage;

    @NotNull
    @PositiveOrZero
    private Integer correctCount;

    @NotNull
    @Positive
    private Integer totalQuestions;

    @NotNull
    @PositiveOrZero
    private Integer timeTakenSeconds;
}