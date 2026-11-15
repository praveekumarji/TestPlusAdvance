package com.testpulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MegaTestLeaderboardEntry {
    private int rank;
    private String userId;
    private String userName;
    private int score;
    private int totalQuestions;
    private int timeTakenSeconds;
}
