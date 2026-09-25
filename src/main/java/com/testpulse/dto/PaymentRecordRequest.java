package com.testpulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRecordRequest {
    @NotNull
    private Long userId;
    @NotNull
    private Integer classId;
    private String userMobile;
    private String userEmail;
    private String userName;

    @NotBlank
    private String planId;

    private String planCode;
    private String planTitle;

    @NotNull
    private Integer durationDays;

    private Long originalAmountInPaise;
    private Long discountAmountInPaise;
    @NotNull
    private Long amountInPaise;

    private String couponCode;

    private String currency;

    @NotBlank
    private String paymentStatus;

    private String paymentProvider;

    @JsonAlias("purchaseToken")
    private String googlePurchaseToken;

    private String googleProductId;

    @JsonAlias("googlePlayOrderId")
    private String googleOrderId;
    private String errorMessage;
}