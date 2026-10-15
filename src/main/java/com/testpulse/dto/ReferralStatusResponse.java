package com.testpulse.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralStatusResponse {

    private boolean success;

    @JsonProperty("is_locked")
    private boolean isLocked;

    @JsonProperty("referral_id")
    private String referralId;

    @JsonProperty("teacher_name")
    private String teacherName;

    @JsonProperty("linked_at")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String linkedAt;
}