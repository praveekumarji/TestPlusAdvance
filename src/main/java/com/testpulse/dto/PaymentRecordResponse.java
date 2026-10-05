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
public class PaymentRecordResponse {
    private boolean success;
    private String message;
    private String subscriptionStatus;
    private UserSubscriptionResponse user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSubscriptionResponse {
        private Long id;
        private String email;
        private String fullName;
        private String mobileNumber;
        private Long classId;
        private String className;
        private String subscriptionStatus;
        private String subscriptionPlan;
        private String subscriptionExpiry;
        private List<Long> paidClassIds;
        private boolean hasUsedTrial;
        private String role;
    }
}