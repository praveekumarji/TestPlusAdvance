package com.testpulse.service.impl;

import com.testpulse.model.Payment;
import com.testpulse.model.PaymentStatus;
import com.testpulse.model.SubscriptionStatus;
import com.testpulse.model.User;
import com.testpulse.dto.PaymentRecordRequest;
import com.testpulse.dto.PaymentRecordResponse;
import com.testpulse.repository.PaymentRepository;
import com.testpulse.repository.SubscriptionPlanRepository;
import com.testpulse.repository.UserRepository;
import com.testpulse.service.PaymentService;
import com.testpulse.service.GooglePlayBillingVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final GooglePlayBillingVerificationService googlePlayBillingVerificationService;
    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository, UserRepository userRepository,
                              SubscriptionPlanRepository subscriptionPlanRepository,
                              GooglePlayBillingVerificationService googlePlayBillingVerificationService) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.googlePlayBillingVerificationService = googlePlayBillingVerificationService;
    }

    public PaymentServiceImpl(PaymentRepository paymentRepository, UserRepository userRepository) {
        this(paymentRepository, userRepository, null, null);
    }

    @Override
    @Transactional
    public Payment initiatePayment(Long userId, String planName, BigDecimal amount) {
        return initiatePayment(userId, null, planName, amount);
    }

    @Override
    @Transactional
    public Payment initiatePayment(Long userId, String planId, String planName, BigDecimal amount) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required.");
        }
        if (planName == null || planName.isBlank()) {
            throw new IllegalArgumentException("Plan name is required.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        User user = userRepository == null ? null : userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        validatePlanForUser(user, planId);

        Payment payment = Payment.builder()
                .user(user)
            .planId(planId)
                .planName(planName)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found."));
    }

    @Override
    @Transactional
    public Payment recordSuccess(Long paymentId, String googlePurchaseToken) {
        Payment payment = getPaymentById(paymentId);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setGooglePurchaseToken(googlePurchaseToken);
        payment.setPaidAt(LocalDateTime.now());
        payment.setFailureReason(null);
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public Payment recordFailure(Long paymentId, String failureReason) {
        Payment payment = getPaymentById(paymentId);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason == null || failureReason.isBlank()
                ? "Payment failed or was cancelled by the user."
                : failureReason);
        payment.setPaidAt(null);
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public PaymentRecordResponse recordPayment(PaymentRecordRequest request) {
        log.info("Recording payment: {}", request);
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required.");
        }
        if (request.getAmountInPaise() == null || request.getAmountInPaise() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (request.getDurationDays() == null || request.getDurationDays() <= 0) {
            throw new IllegalArgumentException("Duration days must be greater than zero.");
        }
        if (request.getPlanId() == null || request.getPlanId().isBlank()) {
          //  throw new IllegalArgumentException("Plan ID is required.");
        }

        String requestedStatus = request.getPaymentStatus().trim().toUpperCase(Locale.ROOT);
        log.info("Requested payment status: {}", requestedStatus);
        PaymentStatus storedStatus = switch (requestedStatus) {
            case "INITIATED" -> PaymentStatus.PENDING;
            case "SUCCESS" -> PaymentStatus.SUCCESS;
            case "FAILED" -> PaymentStatus.FAILED;
            case "CANCELLED" -> PaymentStatus.CANCELLED;
            default -> throw new IllegalArgumentException("paymentStatus must be INITIATED, SUCCESS, FAILED, or CANCELLED.");
        };

        if (request.getClassId() == null || (request.getClassId() != 1 && request.getClassId() != 2)) {
            throw new IllegalArgumentException("Only classId 1 or 2 is supported for subscription payments.");
        }

        GooglePlayBillingVerificationService.VerificationResult verification = null;
        if (storedStatus == PaymentStatus.SUCCESS) {
            if (googlePlayBillingVerificationService == null) {
                throw new IllegalStateException("Google Play verification is not available.");
            }
            String purchaseToken = request.getGooglePurchaseToken();
            if (purchaseToken == null || purchaseToken.isBlank()
                    || paymentRepository.existsByGooglePurchaseToken(purchaseToken.trim())) {
                throw new IllegalArgumentException("Google Play purchase token is missing or already used.");
            }
        //    verification = googlePlayBillingVerificationService.verifyAndAcknowledge(request);
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
      //  validatePlanForUser(user, request.getPlanId());
        log.info("Storing payment record for userId: {}, planId: {}, amountInPaise: {}, status: {}",
                user.getId(), request.getPlanId(), request.getAmountInPaise(), storedStatus);
        Payment payment = Payment.builder()
                .user(user)
                .planName(request.getPlanTitle() == null || request.getPlanTitle().isBlank()
                        ? request.getPlanId() : request.getPlanTitle())
                .planId(request.getPlanId())
                .planCode(request.getPlanCode())
                .planTitle(request.getPlanTitle())
                .durationDays(request.getDurationDays())
                .originalAmountInPaise(request.getOriginalAmountInPaise())
                .discountAmountInPaise(request.getDiscountAmountInPaise())
                .amountInPaise(request.getAmountInPaise())
                .couponCode(request.getCouponCode())
                .amount(BigDecimal.valueOf(request.getAmountInPaise(), 2))
                .currency(request.getCurrency() == null ? "INR" : request.getCurrency())
                .status(storedStatus)
                .paymentProvider(request.getPaymentProvider() == null ? "GOOGLE_PLAY" : request.getPaymentProvider())
                .googlePurchaseToken(request.getGooglePurchaseToken())
                .googleProductId(verification == null ? request.getGoogleProductId() : verification.productId())
                .googleOrderId(verification == null ? request.getGoogleOrderId() : verification.orderId())
                .failureReason(request.getErrorMessage())
                .createdAt(LocalDateTime.now())
                .paidAt(storedStatus == PaymentStatus.SUCCESS ? LocalDateTime.now() : null)
                .build();
        paymentRepository.save(payment);
        log.info("Payment record stored successfully for userId: {}, paymentId: {}", user.getId(), payment.getId());
        if (storedStatus == PaymentStatus.SUCCESS) {
            log.info("Updating user subscription for userId: {}", user.getId());
            //get classId from user education class and set it to subscriptionClassId
            int classId = request.getClassId();
            if (classId == 1) {
                user.setClass1SubscriptionStatus("PAID");
            } else {
                user.setClass2SubscriptionStatus("PAID");
            }
            user.setSubscriptionStatus(SubscriptionStatus.PAID);
            user.setSubscriptionClassId(user.getEducationClass() == null ? null : user.getEducationClass().getId());
            log.info("seting user subscription status to PAID for userId: {}", user.getId());
            user.setSubscriptionPlan(request.getPlanCode() == null ? request.getPlanId() : request.getPlanCode());
            user.setSubscriptionExpiry(LocalDateTime.now().plusDays(request.getDurationDays()));
            userRepository.save(user);
            log.info("User subscription updated successfully for userId: {}", user.getId());
        }

        return PaymentRecordResponse.builder()
                .success(true)
                .message("Payment record stored successfully")
                .paymentStatus(requestedStatus)
                .user(PaymentRecordResponse.UserSubscriptionResponse.builder()
                        .id(user.getId())
                        .subscriptionStatus(user.getEffectiveSubscriptionStatus().name())
                        .subscriptionPlan(user.getSubscriptionPlan())
                        .build())
                .build();
    }

    private void validatePlanForUser(User user, String planId) {
        if (planId == null || planId.isBlank() || subscriptionPlanRepository == null) {
            return;
        }

        com.testpulse.model.SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found."));
        if (user == null || user.getEducationClass() == null) {
            return;
        }
        boolean supported = plan.getEducationClasses().stream()
                .anyMatch(educationClass -> educationClass.getId().equals(user.getEducationClass().getId()));
        if (!supported) {
            throw new IllegalArgumentException("This subscription plan is not available for the user's class.");
        }
    }

}
