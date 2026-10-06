package com.testpulse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpulse.controller.PaymentController;
import com.testpulse.dto.PaymentRecordRequest;
import com.testpulse.dto.PaymentRecordResponse;
import com.testpulse.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerRoutingTest {

        @AfterEach
        void clearSecurityContext() {
                SecurityContextHolder.clearContext();
        }

    @Test
    void shouldAcceptRecordsSuccessRoute() throws Exception {
        PaymentService paymentService = mock(PaymentService.class);
        when(paymentService.recordPayment(any(PaymentRecordRequest.class), eq(2L)))
                .thenReturn(PaymentRecordResponse.builder()
                        .success(true)
                        .message("Payment verified and subscription activated successfully")
                        .subscriptionStatus("PAID")
                        .user(PaymentRecordResponse.UserSubscriptionResponse.builder()
                                .id(2L)
                                .email("rahul@example.com")
                                .fullName("Rahul Sharma")
                                .mobileNumber("9876543210")
                                .classId(1L)
                                .className("BIHAR BOARD 10")
                                .subscriptionStatus("PAID")
                                .subscriptionPlan("Class 10th Full Access Pass")
                                .subscriptionExpiry("2027-10-04T23:59:59Z")
                                .paidClassIds(List.of(1L))
                                .hasUsedTrial(true)
                                .role("USER")
                                .build())
                        .build());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PaymentController(paymentService)).build();
        ObjectMapper objectMapper = new ObjectMapper();

        PaymentRecordRequest request = new PaymentRecordRequest();
        request.setUserId(2L);
        request.setUserEmail("rahul@example.com");
        request.setClassId(1);
        request.setPlanId("plan_annual");
        request.setDurationDays(210);
        request.setAmountInPaise(5900L);
        request.setPaymentStatus("SUCCESS");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("2", null, List.of()));

        mockMvc.perform(post("/api/payment/records-success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("Payment verified and subscription activated successfully"))
                .andExpect(jsonPath("$.subscriptionStatus").value("PAID"))
                .andExpect(jsonPath("$.user.id").value(2))
                                .andExpect(jsonPath("$.user.email").value("rahul@example.com"))
                                .andExpect(jsonPath("$.user.fullName").value("Rahul Sharma"))
                .andExpect(jsonPath("$.user.mobileNumber").value("9876543210"))
                .andExpect(jsonPath("$.user.classId").value(1))
                .andExpect(jsonPath("$.user.className").value("BIHAR BOARD 10"))
                .andExpect(jsonPath("$.user.subscriptionStatus").value("PAID"))
                                .andExpect(jsonPath("$.user.subscriptionPlan").value("Class 10th Full Access Pass"))
                                .andExpect(jsonPath("$.user.subscriptionExpiry").value("2027-10-04T23:59:59Z"))
                                .andExpect(jsonPath("$.user.paidClassIds[0]").value(1))
                .andExpect(jsonPath("$.user.hasUsedTrial").value(true))
                .andExpect(jsonPath("$.user.role").value("USER"))
                                .andExpect(jsonPath("$.paymentStatus").doesNotExist())
                                .andExpect(jsonPath("$.userDetails").doesNotExist());
    }

        @Test
        void shouldRejectPaymentRecordForDifferentAuthenticatedUser() throws Exception {
                PaymentService paymentService = mock(PaymentService.class);
                MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PaymentController(paymentService)).build();
                ObjectMapper objectMapper = new ObjectMapper();

                PaymentRecordRequest request = new PaymentRecordRequest();
                request.setUserId(99L);
                request.setClassId(1);
                request.setPlanId("plan_annual");
                request.setDurationDays(210);
                request.setAmountInPaise(5900L);
                request.setPaymentStatus("SUCCESS");
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("2", null, List.of()));

                mockMvc.perform(post("/api/payment/records-success")
                                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }
}
