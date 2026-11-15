package com.testpulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MegaTestAttemptResponse {
    private boolean success;
    private boolean alreadySubmitted;
    private String message;
    private int rank;
    private int totalParticipants;
    private BigDecimal percentile;
    private int score;
    private int totalQuestions;
    private int timeTakenSeconds;
}
