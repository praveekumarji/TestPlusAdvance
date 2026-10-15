package com.testpulse;

import com.testpulse.model.Referral;
import com.testpulse.model.User;
import com.testpulse.model.UserReferralApplication;
import com.testpulse.repository.ReferralRepository;
import com.testpulse.repository.UserReferralApplicationRepository;
import com.testpulse.repository.UserRepository;
import com.testpulse.service.ReferralApplicationService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReferralApplicationServiceTest {

    @Test
    void shouldReturnUnlockedStatusWhenUserHasNoApplication() {
        UserRepository userRepository = mock(UserRepository.class);
        ReferralRepository referralRepository = mock(ReferralRepository.class);
        UserReferralApplicationRepository applicationRepository = mock(UserReferralApplicationRepository.class);
        when(applicationRepository.findByUser_Id(987654L)).thenReturn(Optional.empty());

        var response = new ReferralApplicationService(userRepository, referralRepository, applicationRepository)
                .getStatus(987654L);

        assertEquals(true, response.isSuccess());
        assertEquals(false, response.isLocked());
        assertEquals(null, response.getReferralId());
        assertEquals(null, response.getTeacherName());
        assertEquals(null, response.getLinkedAt());
    }

    @Test
    void shouldReturnLinkedReferralStatusWhenApplicationExists() {
        Referral referral = Referral.builder()
                .referralId("RAMESH10")
                .teacherName("Ramesh Sharma Sir")
                .build();
        UserReferralApplication application = UserReferralApplication.builder()
                .referral(referral)
                .locked(true)
                .createdTimestamp(java.time.LocalDateTime.now())
                .build();
        UserRepository userRepository = mock(UserRepository.class);
        ReferralRepository referralRepository = mock(ReferralRepository.class);
        UserReferralApplicationRepository applicationRepository = mock(UserReferralApplicationRepository.class);
        when(applicationRepository.findByUser_Id(987654L)).thenReturn(Optional.of(application));

        var response = new ReferralApplicationService(userRepository, referralRepository, applicationRepository)
                .getStatus(987654L);

        assertEquals(true, response.isLocked());
        assertEquals("RAMESH10", response.getReferralId());
        assertEquals("Ramesh Sharma Sir", response.getTeacherName());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getLinkedAt());
    }

    @Test
    void shouldLinkAndLockReferralForAuthenticatedUser() {
        User user = User.builder().id(987654L).mobileNumber("9876543210").build();
        Referral referral = Referral.builder()
                .id(3L)
                .referralId("RAMESH10")
                .teacherName("Ramesh Sharma Sir")
                .mobileNo("9123456789")
                .build();
        UserRepository userRepository = mock(UserRepository.class);
        ReferralRepository referralRepository = mock(ReferralRepository.class);
        UserReferralApplicationRepository applicationRepository = mock(UserReferralApplicationRepository.class);
        when(applicationRepository.findByUser_Id(987654L)).thenReturn(Optional.empty());
        when(userRepository.findById(987654L)).thenReturn(Optional.of(user));
        when(referralRepository.findByReferralIdIgnoreCase("RAMESH10")).thenReturn(Optional.of(referral));
        when(applicationRepository.save(any(UserReferralApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = new ReferralApplicationService(userRepository, referralRepository, applicationRepository)
                .apply(987654L, " RAMESH10 ");

        assertEquals(true, response.isSuccess());
        assertEquals("Referral code linked successfully!", response.getMessage());
        assertEquals(true, response.isLocked());
        assertEquals("RAMESH10", response.getReferralId());
        assertEquals("Ramesh Sharma Sir", response.getTeacherName());
        verify(applicationRepository).save(any(UserReferralApplication.class));
    }

    @Test
    void shouldRejectApplyingAnotherReferralAfterOneIsLocked() {
        UserRepository userRepository = mock(UserRepository.class);
        ReferralRepository referralRepository = mock(ReferralRepository.class);
        UserReferralApplicationRepository applicationRepository = mock(UserReferralApplicationRepository.class);
        when(applicationRepository.findByUser_Id(987654L))
                .thenReturn(Optional.of(UserReferralApplication.builder().locked(true).build()));

        assertEquals("Referral code has already been locked for this account.",
                assertThrows(IllegalStateException.class,
                        () -> new ReferralApplicationService(userRepository, referralRepository, applicationRepository)
                                .apply(987654L, "RAMESH10")).getMessage());
    }

    @Test
    void shouldRejectSelfReferralUsingNormalizedPhoneNumbers() {
        User user = User.builder().id(987654L).mobileNumber("+91 98765-43210").build();
        Referral referral = Referral.builder().referralId("OWNCODE").mobileNo("9876543210").build();
        UserRepository userRepository = mock(UserRepository.class);
        ReferralRepository referralRepository = mock(ReferralRepository.class);
        UserReferralApplicationRepository applicationRepository = mock(UserReferralApplicationRepository.class);
        when(applicationRepository.findByUser_Id(987654L)).thenReturn(Optional.empty());
        when(userRepository.findById(987654L)).thenReturn(Optional.of(user));
        when(referralRepository.findByReferralIdIgnoreCase("OWNCODE")).thenReturn(Optional.of(referral));

        assertEquals("You cannot use your own referral code.",
                assertThrows(IllegalArgumentException.class,
                        () -> new ReferralApplicationService(userRepository, referralRepository, applicationRepository)
                                .apply(987654L, "OWNCODE")).getMessage());
    }
}
