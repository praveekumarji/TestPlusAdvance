package com.testpulse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReferralApplyRequest {

    @JsonProperty("user_id")
    @NotBlank
    @Size(max = 50)
    private String userId;

    @JsonProperty("referral_id")
    @NotBlank
    @Size(max = 50)
    private String referralId;
}