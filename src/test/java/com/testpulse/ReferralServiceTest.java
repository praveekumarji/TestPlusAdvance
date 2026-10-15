package com.testpulse;

import com.testpulse.dto.ReferralRequest;
import com.testpulse.model.Referral;
import com.testpulse.repository.ReferralRepository;
import com.testpulse.service.ReferralService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReferralServiceTest {

    @Test
    void shouldCreateReferralWithTrimmedFields() {
        ReferralRepository repository = mock(ReferralRepository.class);
        when(repository.existsByReferralId("RAMESH10")).thenReturn(false);
        when(repository.save(any(Referral.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Referral saved = new ReferralService(repository).create(request());

        assertEquals("RAMESH10", saved.getReferralId());
        assertEquals("Ramesh Sharma Sir", saved.getTeacherName());
        assertEquals("AGENT10", saved.getAgentCode());
        assertEquals("Agent Name", saved.getAgentName());
        assertEquals("9123456789", saved.getAgentMobile());
        assertEquals("9876543210", saved.getMobileNo());
        assertEquals("Patna", saved.getAddress());
    }

    @Test
    void shouldAllowReferralWithoutOptionalAgentFields() {
        ReferralRepository repository = mock(ReferralRepository.class);
        when(repository.existsByReferralId("RAMESH10")).thenReturn(false);
        when(repository.save(any(Referral.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReferralRequest request = request();
        request.setAgentCode(null);
        request.setAgentName(null);
        request.setAgentMobile(null);
        Referral saved = new ReferralService(repository).create(request);

        assertNull(saved.getAgentCode());
        assertNull(saved.getAgentName());
        assertNull(saved.getAgentMobile());
    }

    @Test
    void shouldRejectDuplicateReferralId() {
        ReferralRepository repository = mock(ReferralRepository.class);
        when(repository.existsByReferralId("RAMESH10")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> new ReferralService(repository).create(request()));
    }

    private ReferralRequest request() {
        ReferralRequest request = new ReferralRequest();
        request.setReferralId(" RAMESH10 ");
        request.setTeacherName(" Ramesh Sharma Sir ");
        request.setAgentCode(" AGENT10 ");
        request.setAgentName(" Agent Name ");
        request.setAgentMobile(" 9123456789 ");
        request.setMobileNo(" 9876543210 ");
        request.setAddress(" Patna ");
        return request;
    }
}