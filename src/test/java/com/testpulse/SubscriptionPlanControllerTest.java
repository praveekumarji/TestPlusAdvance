package com.testpulse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpulse.controller.SubscriptionPlanController;
import com.testpulse.model.EducationClass;
import com.testpulse.model.SubscriptionPlan;
import com.testpulse.service.SubscriptionPlanService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubscriptionPlanControllerTest {

    @Test
    void shouldReturnClassIdsInsteadOfNestedEducationClasses() throws Exception {
        SubscriptionPlanService service = mock(SubscriptionPlanService.class);
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .id("PLAN_001")
                .planCode("BASIC")
                .title("Basic Plan")
                .subtitle("Starter")
                .durationDays(30)
                .originalAmountInPaise(19900L)
                .discountedAmountInPaise(9900L)
                .displayPrice("₹99")
                .displayOriginalPrice("₹199")
                .discountPercentage(50)
                .badge("Popular")
                .isRecommended(false)
                .features(List.of("Live classes", "Practice tests"))
                .educationClasses(Set.of(EducationClass.builder().id(1L).code("C1").name("Class 1").active(true).build()))
                .build();

        when(service.getAllPlans()).thenReturn(List.of(plan));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SubscriptionPlanController(service)).build();

        mockMvc.perform(get("/api/subscription-plans").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"classId\":1")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\"educationClasses\""))));
    }
}
