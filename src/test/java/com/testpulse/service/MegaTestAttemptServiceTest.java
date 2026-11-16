package com.testpulse.service;

import com.testpulse.dto.CustomTestPreviewResponse;
import com.testpulse.dto.MegaTestAttemptResponse;
import com.testpulse.dto.MegaTestLeaderboardResponse;
import com.testpulse.dto.SubmitMegaTestAttemptRequest;
import com.testpulse.model.MegaTestAttempt;
import com.testpulse.model.MegaTestDetails;
import com.testpulse.repository.MegaTestAttemptRepository;
import com.testpulse.repository.MegaTestDetailsRepository;
import com.testpulse.service.impl.MegaTestAttemptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MegaTestAttemptServiceTest {

    private static final String TEST_ID = "105";
    private static final String USER_ID = "4821";
    private static final int CLASS_ID = 1;

    private MegaTestAttemptRepository attemptRepository;
    private MegaTestDetailsRepository detailsRepository;
    private MegaTestAttemptServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        attemptRepository = mock(MegaTestAttemptRepository.class);
        detailsRepository = mock(MegaTestDetailsRepository.class);
        service = new MegaTestAttemptServiceImpl(attemptRepository, detailsRepository, new ObjectMapper(), 5);
        when(detailsRepository.findByIdForUpdate(105L)).thenReturn(Optional.of(
                MegaTestDetails.builder()
                        .id(105L)
                        .classId(1L)
                        .responseJson(new ObjectMapper().writeValueAsString(preview(50)))
                        .build()));
        when(detailsRepository.findById(105L)).thenReturn(Optional.of(
                MegaTestDetails.builder()
                        .id(105L)
                        .classId(1L)
                        .responseJson(new ObjectMapper().writeValueAsString(preview(50)))
                        .build()));
        when(attemptRepository.findByTestIdAndUserId(TEST_ID, USER_ID)).thenReturn(Optional.empty());
    }

    @Test
    void storesFirstSubmissionAndCalculatesStandings() {
        when(attemptRepository.countRankedAheadOfNewAttempt(
                eq(TEST_ID), eq(44), eq(1145), any(LocalDateTime.class))).thenReturn(13L);
        when(attemptRepository.countByTestId(TEST_ID)).thenReturn(1249L);
        when(attemptRepository.saveAndFlush(any(MegaTestAttempt.class))).thenAnswer(invocation -> {
            MegaTestAttempt saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        MegaTestAttemptResponse response = service.submitAttempt(request(), null);

        assertEquals(false, response.isAlreadySubmitted());
        assertEquals("Attempt submitted successfully", response.getMessage());
        assertEquals(14, response.getRank());
        assertEquals(1250, response.getTotalParticipants());
        assertEquals(new BigDecimal("98.8"), response.getPercentile());
        assertEquals(44, response.getScore());
        assertEquals(50, response.getTotalQuestions());
        assertEquals(1145, response.getTimeTakenSeconds());
        verify(detailsRepository).findByIdForUpdate(105L);
        verify(attemptRepository).saveAndFlush(any(MegaTestAttempt.class));
    }

    @Test
    void existingAttemptReturnsOriginalStandingsAndDoesNotOverwrite() {
        MegaTestAttempt existing = MegaTestAttempt.builder()
                .testId(TEST_ID)
                .userId(USER_ID)
                .score(42)
                .totalQuestions(50)
                .timeTakenSeconds(1200)
                .rankAtSubmission(16)
                .participantsAtSubmission(100)
                .percentileAtSubmission(new BigDecimal("84.0"))
                .build();
        when(attemptRepository.findByTestIdAndUserId(TEST_ID, USER_ID)).thenReturn(Optional.of(existing));

        MegaTestAttemptResponse response = service.submitAttempt(request(), null);

        assertEquals(true, response.isAlreadySubmitted());
        assertEquals("Attempt already submitted", response.getMessage());
        assertEquals(16, response.getRank());
        assertEquals(100, response.getTotalParticipants());
        assertEquals(new BigDecimal("84.0"), response.getPercentile());
        assertEquals(42, response.getScore());
        verify(attemptRepository, never()).saveAndFlush(any(MegaTestAttempt.class));
        verify(attemptRepository, never()).countRankedAheadOfNewAttempt(any(), anyInt(), anyInt(), any());
    }

    @Test
    void authenticatedIdentityMustMatchRequestUser() {
        assertThrows(IllegalArgumentException.class,
                () -> service.submitAttempt(request(), 999L));

        verify(detailsRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void rejectsAttemptWithQuestionCountsThatDoNotAddUp() {
        SubmitMegaTestAttemptRequest invalid = request();
        invalid.setSkippedCount(1);

        assertThrows(IllegalArgumentException.class, () -> service.submitAttempt(invalid, null));
        verify(attemptRepository, never()).saveAndFlush(any(MegaTestAttempt.class));
    }

    @Test
    void returnsTopRankersAndRequestedUsersCurrentRank() {
        MegaTestAttempt top = attempt("9012", "Priya Kumari", 50, 890,
                LocalDateTime.parse("2026-11-14T10:00:00"), 10L);
        MegaTestAttempt second = attempt("3310", "Aman Raj", 49, 940,
                LocalDateTime.parse("2026-11-14T10:01:00"), 11L);
        MegaTestAttempt mine = attempt(USER_ID, "Rahul Kumar", 44, 1145,
                LocalDateTime.parse("2026-11-14T10:10:00"), 12L);
        when(attemptRepository.countByTestId(TEST_ID)).thenReturn(1250L);
        when(attemptRepository.findByTestIdOrderByScoreDescTimeTakenSecondsAscCreatedAtAscIdAsc(
                eq(TEST_ID), any())).thenReturn(List.of(top, second));
        when(attemptRepository.findByTestIdAndUserId(TEST_ID, USER_ID)).thenReturn(Optional.of(mine));
        when(attemptRepository.countRankedAhead(TEST_ID, 44, 1145, mine.getCreatedAt(), mine.getId()))
                .thenReturn(13L);

        MegaTestLeaderboardResponse response = service.getLeaderboard(TEST_ID, CLASS_ID, USER_ID);

        assertEquals(true, response.isSuccess());
        assertEquals(TEST_ID, response.getTestId());
        assertEquals("All-Bihar Sunday Mega Test #12", response.getTestTitle());
        assertEquals(1L, response.getClassId());
        assertEquals(1250L, response.getTotalParticipants());
        assertEquals(2, response.getLeaderboard().size());
        assertEquals(1, response.getLeaderboard().get(0).getRank());
        assertEquals("Priya Kumari", response.getLeaderboard().get(0).getUserName());
        assertEquals(2, response.getLeaderboard().get(1).getRank());
        assertEquals(14, response.getMyEntry().getRank());
        assertEquals(new BigDecimal("98.9"), response.getMyEntry().getPercentile());
    }

    @Test
    void omitsMyEntryWhenUserHasNoAttempt() {
        when(attemptRepository.countByTestId(TEST_ID)).thenReturn(0L);
        when(attemptRepository.findByTestIdOrderByScoreDescTimeTakenSecondsAscCreatedAtAscIdAsc(
                eq(TEST_ID), any())).thenReturn(List.of());
        when(attemptRepository.findByTestIdAndUserId(TEST_ID, USER_ID)).thenReturn(Optional.empty());

        MegaTestLeaderboardResponse response = service.getLeaderboard(TEST_ID, null, USER_ID);

        assertEquals(null, response.getMyEntry());
    }

    @Test
    void rejectsLeaderboardRequestForDifferentClass() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getLeaderboard(TEST_ID, 2, null));

        verify(attemptRepository, never()).countByTestId(any());
    }

    @Test
    void findsAvailableTestForClassRegardlessOfItsTestDate() {
        var oldTestDate = java.time.LocalDate.of(2024, 1, 7);
        when(detailsRepository.findFirstByClassIdOrderByTestDateDescIdDesc(1L))
                .thenReturn(Optional.of(MegaTestDetails.builder()
                        .id(105L)
                        .classId(1L)
                        .testDate(oldTestDate)
                        .responseJson(jsonPreview())
                        .build()));
        when(attemptRepository.countByTestId(TEST_ID)).thenReturn(0L);
        when(attemptRepository.findByTestIdOrderByScoreDescTimeTakenSecondsAscCreatedAtAscIdAsc(
                eq(TEST_ID), any())).thenReturn(List.of());

        MegaTestLeaderboardResponse response = service.getCurrentLeaderboard(1L, null);

        assertEquals(TEST_ID, response.getTestId());
        assertEquals(1L, response.getClassId());
        assertEquals(0L, response.getTotalParticipants());
        verify(detailsRepository).findFirstByClassIdOrderByTestDateDescIdDesc(1L);
    }

    @Test
    void currentLeaderboardReturnsClearErrorWhenClassHasNoTest() {
        when(detailsRepository.findFirstByClassIdOrderByTestDateDescIdDesc(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.getCurrentLeaderboard(1L, null));
        verify(attemptRepository, never()).countByTestId(any());
    }

    private static SubmitMegaTestAttemptRequest request() {
        SubmitMegaTestAttemptRequest request = new SubmitMegaTestAttemptRequest();
        request.setTestId(TEST_ID);
        request.setUserId(USER_ID);
        request.setUserName("Rahul Kumar");
        request.setClassId(CLASS_ID);
        request.setScore(44);
        request.setTotalQuestions(50);
        request.setCorrectCount(44);
        request.setWrongCount(4);
        request.setSkippedCount(2);
        request.setTimeTakenSeconds(1145);
        return request;
    }

    private static CustomTestPreviewResponse preview(int questionCount) {
        return CustomTestPreviewResponse.builder()
                .id("mega-preview")
                .title("All-Bihar Sunday Mega Test #12")
                .subject("Science")
                .totalQuestions(questionCount)
                .questions(List.of())
                .build();
    }

    private static MegaTestAttempt attempt(String userId, String userName, int score,
                                           int timeTakenSeconds, LocalDateTime createdAt, Long id) {
        return MegaTestAttempt.builder()
                .id(id)
                .testId(TEST_ID)
                .userId(userId)
                .userName(userName)
                .classId(CLASS_ID)
                .score(score)
                .totalQuestions(50)
                .timeTakenSeconds(timeTakenSeconds)
                .createdAt(createdAt)
                .build();
    }

    private static String jsonPreview() {
        try {
            return new ObjectMapper().writeValueAsString(preview(50));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
