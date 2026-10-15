package com.testpulse;

import com.testpulse.config.JwtAuthenticationToken;
import com.testpulse.dto.ReferralApplyResponse;
import com.testpulse.dto.ReferralStatusResponse;
import com.testpulse.service.ReferralApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReferralApplicationControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnReferralSuccessPayload() throws Exception {
        ReferralApplicationService service = mock(ReferralApplicationService.class);
        when(service.apply(987654L, "RAMESH10")).thenReturn(ReferralApplyResponse.builder()
                .success(true)
                .message("Referral code linked successfully!")
                .isLocked(true)
                .referralId("RAMESH10")
                .teacherName("Ramesh Sharma Sir")
                .build());
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken("987654", List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new com.testpulse.controller.ReferralApplicationController(service))
                .build();

        mockMvc.perform(post("/api/referral/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":\"usr_987654\",\"referral_id\":\"RAMESH10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Referral code linked successfully!"))
                .andExpect(jsonPath("$.is_locked").value(true))
                .andExpect(jsonPath("$.referral_id").value("RAMESH10"))
                .andExpect(jsonPath("$.teacher_name").value("Ramesh Sharma Sir"));
    }

    @Test
    void shouldRejectRequestUserIdThatDoesNotMatchJwt() throws Exception {
        ReferralApplicationService service = mock(ReferralApplicationService.class);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken("987654", List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new com.testpulse.controller.ReferralApplicationController(service))
                .build();

        mockMvc.perform(post("/api/referral/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":\"usr_123\",\"referral_id\":\"RAMESH10\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(service, never()).apply(eq(987654L), eq("RAMESH10"));
    }

    @Test
    void shouldReturnUnlockedStatusWhenNoQueryUserIdIsProvided() throws Exception {
        ReferralApplicationService service = mock(ReferralApplicationService.class);
        when(service.getStatus(987654L)).thenReturn(ReferralStatusResponse.builder()
                .success(true)
                .isLocked(false)
                .referralId(null)
                .teacherName(null)
                .linkedAt(null)
                .build());
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken("987654", List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new com.testpulse.controller.ReferralApplicationController(service))
                .build();

        mockMvc.perform(get("/api/referral/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.is_locked").value(false))
                .andExpect(jsonPath("$.referral_id").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.teacher_name").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.linked_at").doesNotExist());
    }

    @Test
    void shouldReturnLockedStatusForMatchingQueryUserId() throws Exception {
        ReferralApplicationService service = mock(ReferralApplicationService.class);
        when(service.getStatus(987654L)).thenReturn(ReferralStatusResponse.builder()
                .success(true)
                .isLocked(true)
                .referralId("RAMESH10")
                .teacherName("Ramesh Sharma Sir")
                .linkedAt("2026-10-14T10:30:00Z")
                .build());
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken("987654", List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new com.testpulse.controller.ReferralApplicationController(service))
                .build();

        mockMvc.perform(get("/api/referral/status").queryParam("user_id", "usr_987654"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_locked").value(true))
                .andExpect(jsonPath("$.referral_id").value("RAMESH10"))
                .andExpect(jsonPath("$.teacher_name").value("Ramesh Sharma Sir"))
                .andExpect(jsonPath("$.linked_at").value("2026-10-14T10:30:00Z"));
    }

    @Test
    void shouldRejectStatusQueryForDifferentUser() throws Exception {
        ReferralApplicationService service = mock(ReferralApplicationService.class);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken("987654", List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new com.testpulse.controller.ReferralApplicationController(service))
                .build();

        mockMvc.perform(get("/api/referral/status").queryParam("user_id", "usr_123"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(service, never()).getStatus(987654L);
    }
}
