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
public class MegaTestLeaderboardResponse {
    private boolean success;
    private String testId;
    private String testTitle;
    private Long classId;
    private long totalParticipants;
    private MegaTestStudentEntry myEntry;
    private List<MegaTestLeaderboardEntry> leaderboard;
}
