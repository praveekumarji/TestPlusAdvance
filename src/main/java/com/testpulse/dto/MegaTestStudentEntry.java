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
public class MegaTestStudentEntry {
    private int rank;
    private String userId;
    private String userName;
    private int score;
    private int totalQuestions;
    private int timeTakenSeconds;
    private BigDecimal percentile;
}
