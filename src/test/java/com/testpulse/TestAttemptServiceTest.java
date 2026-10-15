package com.testpulse;

import com.testpulse.dto.SubmitTestAttemptRequest;
import com.testpulse.model.EducationClass;
import com.testpulse.model.TestAttempt;
import com.testpulse.repository.TestAttemptRepository;
import com.testpulse.repository.TestRepository;
import com.testpulse.service.impl.TestAttemptServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestAttemptServiceTest {

    @Test
    void shouldMapNewRequestToAttemptUsingAuthenticatedUser() {
        TestAttemptRepository attemptRepository = mock(TestAttemptRepository.class);
        TestRepository testRepository = mock(TestRepository.class);
        com.testpulse.model.Test test = com.testpulse.model.Test.builder()
                .id(101L)
                .title("\u0935\u093f\u091c\u094d\u091e\u093e\u0928 \u092e\u0949\u0921\u0932 \u092a\u0947\u092a\u0930 1")
                .subject("Science")
                .educationClass(EducationClass.builder().id(1L).name("BIHAR BOARD 10").build())
                .build();
        when(testRepository.findById(101L)).thenReturn(Optional.of(test));
        when(attemptRepository.save(any(TestAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TestAttempt saved = new TestAttemptServiceImpl(attemptRepository, testRepository)
                .submitTestAttempt(request(), 42L);

        assertEquals("42", saved.getUserId());
        assertEquals("test_101", saved.getTestId());
        assertEquals(1, saved.getClassId());
        assertEquals(80, saved.getScorePercentage());
        assertEquals(20, saved.getCorrectCount());
        assertEquals(25, saved.getTotalQuestions());
        assertEquals(1200, saved.getTimeTakenSeconds());
        assertNotNull(saved.getCreatedAt());
        verify(attemptRepository).save(saved);
    }

    @Test
    void shouldRejectRequestWhenClassDoesNotMatchTest() {
        TestAttemptRepository attemptRepository = mock(TestAttemptRepository.class);
        TestRepository testRepository = mock(TestRepository.class);
        com.testpulse.model.Test test = com.testpulse.model.Test.builder()
                .id(101L)
                .educationClass(EducationClass.builder().id(2L).name("BIHAR BOARD 12").build())
                .build();
        when(testRepository.findById(101L)).thenReturn(Optional.of(test));

        assertThrows(IllegalArgumentException.class,
                () -> new TestAttemptServiceImpl(attemptRepository, testRepository)
                        .submitTestAttempt(request(), 42L));
    }

    private SubmitTestAttemptRequest request() {
        SubmitTestAttemptRequest request = new SubmitTestAttemptRequest();
        request.setTestId("test_101");
        request.setTestTitle("\u0935\u093f\u091c\u094d\u091e\u093e\u0928 \u092e\u0949\u0921\u0932 \u092a\u0947\u092a\u0930 1");
        request.setClassId(1);
        request.setSubject("Science");
        request.setScorePercentage(80);
        request.setCorrectCount(20);
        request.setTotalQuestions(25);
        request.setTimeTakenSeconds(1200);
        return request;
    }
}
