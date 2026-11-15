package com.testpulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitMegaTestAttemptRequest {

    @NotBlank
    @Size(max = 50)
    private String testId;

    @NotBlank
    @Size(max = 50)
    private String userId;

    @NotBlank
    @Size(max = 255)
    private String userName;

    @NotNull
    @Positive
    private Integer classId;

    @NotNull
    @PositiveOrZero
    private Integer score;

    @NotNull
    @Positive
    private Integer totalQuestions;

    @NotNull
    @PositiveOrZero
    private Integer correctCount;

    @NotNull
    @PositiveOrZero
    private Integer wrongCount;

    @NotNull
    @PositiveOrZero
    private Integer skippedCount;

    @NotNull
    @PositiveOrZero
    private Integer timeTakenSeconds;
}
