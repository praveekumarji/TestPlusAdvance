package com.testpulse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReferralRequest {

    @JsonProperty("referral_id")
    @NotBlank
    @Size(max = 50)
    private String referralId;

    @JsonProperty("teacher_name")
    @NotBlank
    @Size(max = 150)
    private String teacherName;

    @JsonProperty("agentcode")
    @Size(max = 50)
    private String agentCode;

    @JsonProperty("agent_name")
    @Size(max = 150)
    private String agentName;

    @JsonProperty("agent_mobile")
    @Size(max = 30)
    private String agentMobile;

    @JsonProperty("mobile_no")
    @NotBlank
    @Size(max = 30)
    private String mobileNo;

    @Size(max = 500)
    private String address;
}