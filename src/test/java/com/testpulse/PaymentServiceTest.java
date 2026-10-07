package com.testpulse;

import com.testpulse.dto.PaymentRecordRequest;
import com.testpulse.dto.PaymentRecordResponse;
import com.testpulse.model.EducationClass;
import com.testpulse.model.Payment;
import com.testpulse.model.User;
import com.testpulse.repository.PaymentRepository;
import com.testpulse.repository.UserRepository;
import com.testpulse.service.PaymentService;
import com.testpulse.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    @Test
    void shouldTrackPaymentLifecycle() {
        Map<Long, Payment> paymentsById = new HashMap<>();
        Map<Long, User> usersById = new HashMap<>();
        usersById.put(42L, User.builder().id(42L).mobileNumber("9876543210").fullName("Demo User").passwordHash("hash").createdAt(LocalDateTime.now()).build());

        PaymentRepository paymentRepository = (PaymentRepository) Proxy.newProxyInstance(
                PaymentRepository.class.getClassLoader(),
                new Class<?>[]{PaymentRepository.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "save":
                            Payment payment = (Payment) args[0];
                            if (payment.getId() == null) {
                                payment.setId((long) (paymentsById.size() + 1));
                            }
                            paymentsById.put(payment.getId(), payment);
                            return payment;
                        case "findById":
                            return Optional.ofNullable(paymentsById.get((Long) args[0]));
                        default:
                            return null;
                    }
                }
        );

        UserRepository userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "findById":
                            return Optional.ofNullable(usersById.get((Long) args[0]));
                        default:
                            return null;
                    }
                }
        );

        PaymentService paymentService = new PaymentServiceImpl(paymentRepository, userRepository);

        Payment payment = paymentService.initiatePayment(42L, "Premium Plan", new BigDecimal("299.00"));

        assertNotNull(payment);
        assertEquals("PENDING", payment.getStatus().name());
        assertEquals("Premium Plan", payment.getPlanName());

            paymentService.recordSuccess(payment.getId(), "google-purchase-token");
        assertEquals("SUCCESS", paymentService.getPaymentById(payment.getId()).getStatus().name());
            assertEquals("google-purchase-token", paymentService.getPaymentById(payment.getId()).getGooglePurchaseToken());

        paymentService.recordFailure(payment.getId(), "User cancelled during checkout");
        assertEquals("FAILED", paymentService.getPaymentById(payment.getId()).getStatus().name());
        assertEquals("User cancelled during checkout", paymentService.getPaymentById(payment.getId()).getFailureReason());
    }

    @Test
    void shouldRejectPaymentRecordWhenEmailDoesNotMatchAuthenticatedUser() {
        User user = User.builder()
                .id(42L)
                .email("student@example.com")
                .fullName("Demo User")
                .passwordHash("hash")
                .createdAt(LocalDateTime.now())
                .build();
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        PaymentService paymentService = new PaymentServiceImpl(mock(PaymentRepository.class), userRepository);
        PaymentRecordRequest request = new PaymentRecordRequest();
        request.setUserId(42L);
        request.setUserEmail("someone-else@example.com");
        request.setClassId(1);
        request.setPlanId("plan_annual");
        request.setDurationDays(210);
        request.setAmountInPaise(5900L);
        request.setPaymentStatus("INITIATED");

        assertThrows(AccessDeniedException.class, () -> paymentService.recordPayment(request, 42L));
    }

    @Test
    void shouldAllowInitiatedPaymentWhenUserHasNoSubscriptionClass() {
        User user = User.builder()
                .id(42L)
                .email("student@example.com")
                .fullName("Demo User")
                .passwordHash("hash")
                .createdAt(LocalDateTime.now())
                .educationClass(EducationClass.builder().name("Class 1").build())
                .subscriptionClassId(null)
                .build();
        UserRepository userRepository = mock(UserRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(paymentRepository.save(org.mockito.ArgumentMatchers.any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentService paymentService = new PaymentServiceImpl(paymentRepository, userRepository);
        PaymentRecordRequest request = new PaymentRecordRequest();
        request.setUserId(42L);
        request.setClassId(1);
        request.setPlanId("plan_annual");
        request.setDurationDays(210);
        request.setAmountInPaise(5900L);
        request.setPaymentStatus("INITIATED");

        PaymentRecordResponse response = paymentService.recordPayment(request, 42L);

        assertNotNull(response);
        assertNull(response.getUser().getClassId());
        assertNull(response.getUser().getClassName());
    }
}
