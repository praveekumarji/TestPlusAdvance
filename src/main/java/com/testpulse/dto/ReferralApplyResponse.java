package com.testpulse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralApplyResponse {

    private boolean success;
    private String message;

    @JsonProperty("is_locked")
    private boolean isLocked;

    @JsonProperty("referral_id")
    private String referralId;

    @JsonProperty("teacher_name")
    private String teacherName;
}