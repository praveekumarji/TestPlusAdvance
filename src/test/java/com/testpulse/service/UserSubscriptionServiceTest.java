package com.testpulse.service;

import com.testpulse.model.EducationClass;
import com.testpulse.model.SubscriptionStatus;
import com.testpulse.model.User;
import com.testpulse.model.UserSubscription;
import com.testpulse.repository.EducationClassRepository;
import com.testpulse.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSubscriptionServiceTest {

    @Test
    void shouldKeepPlanAndExpiryIndependentForEachClass() {
        UserSubscriptionRepository subscriptionRepository = mock(UserSubscriptionRepository.class);
        EducationClassRepository classRepository = mock(EducationClassRepository.class);
        EducationClass firstClass = EducationClass.builder().id(1L).code("C1").name("Class 1").build();
        EducationClass secondClass = EducationClass.builder().id(2L).code("C2").name("Class 2").build();
        User user = User.builder().id(10L).build();
        when(classRepository.findById(1L)).thenReturn(Optional.of(firstClass));
        when(classRepository.findById(2L)).thenReturn(Optional.of(secondClass));
        when(subscriptionRepository.findByUser_IdAndEducationClass_Id(10L, 1L)).thenReturn(Optional.empty());
        when(subscriptionRepository.findByUser_IdAndEducationClass_Id(10L, 2L)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserSubscriptionService service = new UserSubscriptionService(subscriptionRepository, classRepository);
        UserSubscription classOne = service.activate(user, 1L, "plan-one", "CODE_ONE", 30,
                SubscriptionStatus.PAID);
        LocalDateTime classOneExpiry = classOne.getExpiresAt();
        UserSubscription classTwo = service.activate(user, 2L, "plan-two", "CODE_TWO", 90,
                SubscriptionStatus.PAID);

        assertEquals("plan-one", classOne.getPlanId());
        assertEquals("CODE_ONE", classOne.getPlanCode());
        assertEquals(classOneExpiry, classOne.getExpiresAt());
        assertEquals("plan-two", classTwo.getPlanId());
        assertEquals("CODE_TWO", classTwo.getPlanCode());
        assertEquals(SubscriptionStatus.PAID, classTwo.getStatus());
        assertEquals(90, java.time.Duration.between(classTwo.getStartedAt(), classTwo.getExpiresAt()).toDays());
    }

        @Test
        void shouldExposeThePrimaryClassSubscriptionThroughLegacyFields() {
                UserSubscriptionRepository subscriptionRepository = mock(UserSubscriptionRepository.class);
                EducationClassRepository classRepository = mock(EducationClassRepository.class);
                EducationClass firstClass = EducationClass.builder().id(1L).code("C1").name("Class 1").build();
                User user = User.builder()
                                .id(10L)
                                .educationClass(firstClass)
                                .subscriptionStatus(SubscriptionStatus.PAID)
                                .subscriptionPlan("legacy-plan")
                                .subscriptionClassId(2L)
                                .build();
                LocalDateTime expiry = LocalDateTime.now().plusDays(30);
                UserSubscription primaryClassSubscription = UserSubscription.builder()
                                .user(user)
                                .educationClass(firstClass)
                                .status(SubscriptionStatus.PAID)
                                .planId("class-one-plan")
                                .planCode("CLASS_ONE")
                                .expiresAt(expiry)
                                .build();
                when(subscriptionRepository.findByUser_IdAndEducationClass_Id(10L, 1L))
                                .thenReturn(Optional.of(primaryClassSubscription));

                UserSubscriptionService service = new UserSubscriptionService(subscriptionRepository, classRepository);
                User resolved = service.resolvePrimaryClassSubscription(user);

                assertEquals(SubscriptionStatus.PAID, resolved.getEffectiveSubscriptionStatus());
                assertEquals("CLASS_ONE", resolved.getSubscriptionPlan());
                assertEquals(expiry, resolved.getSubscriptionExpiry());
                assertEquals(1L, resolved.getSubscriptionClassId());
        }

        @Test
        void shouldMigrateAnExplicitClassStatusEvenWhenLegacyClassIdIsWrong() {
                UserSubscriptionRepository subscriptionRepository = mock(UserSubscriptionRepository.class);
                EducationClassRepository classRepository = mock(EducationClassRepository.class);
                EducationClass firstClass = EducationClass.builder().id(1L).code("C1").name("Class 1").build();
                User user = User.builder()
                                .id(10L)
                                .educationClass(firstClass)
                                .subscriptionStatus(SubscriptionStatus.PAID)
                                .subscriptionPlan("shared-latest-plan")
                                .subscriptionClassId(2L)
                                .subscriptionExpiry(LocalDateTime.now().plusDays(14))
                                .class1SubscriptionStatus("PAID")
                                .build();
                when(subscriptionRepository.findByUser_IdAndEducationClass_Id(10L, 1L)).thenReturn(Optional.empty());
                when(subscriptionRepository.save(any(UserSubscription.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                UserSubscriptionService service = new UserSubscriptionService(subscriptionRepository, classRepository);
                User resolved = service.resolvePrimaryClassSubscription(user);

                assertEquals(SubscriptionStatus.PAID, resolved.getEffectiveSubscriptionStatus());
                assertEquals(null, resolved.getSubscriptionPlan());
                assertEquals(1L, resolved.getSubscriptionClassId());
                verify(subscriptionRepository).save(any(UserSubscription.class));
        }
}
