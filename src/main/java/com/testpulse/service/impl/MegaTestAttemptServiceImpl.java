package com.testpulse.service.impl;

import com.testpulse.dto.CustomTestPreviewResponse;
import com.testpulse.dto.MegaTestAttemptResponse;
import com.testpulse.dto.MegaTestLeaderboardEntry;
import com.testpulse.dto.MegaTestLeaderboardResponse;
import com.testpulse.dto.MegaTestStudentEntry;
import com.testpulse.dto.SubmitMegaTestAttemptRequest;
import com.testpulse.model.MegaTestAttempt;
import com.testpulse.model.MegaTestDetails;
import com.testpulse.repository.MegaTestAttemptRepository;
import com.testpulse.repository.MegaTestDetailsRepository;
import com.testpulse.service.MegaTestAttemptService;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
public class MegaTestAttemptServiceImpl implements MegaTestAttemptService {

    private final MegaTestAttemptRepository attemptRepository;
    private final MegaTestDetailsRepository detailsRepository;
    private final ObjectMapper objectMapper;
    private final int leaderboardLimit;

    public MegaTestAttemptServiceImpl(MegaTestAttemptRepository attemptRepository,
                                     MegaTestDetailsRepository detailsRepository,
                                     ObjectMapper objectMapper,
                                     @Value("${mega-test.leaderboard-limit:5}") int leaderboardLimit) {
        this.attemptRepository = attemptRepository;
        this.detailsRepository = detailsRepository;
        this.objectMapper = objectMapper;
        if (leaderboardLimit < 1 || leaderboardLimit > 100) {
            throw new IllegalArgumentException("mega-test.leaderboard-limit must be between 1 and 100.");
        }
        this.leaderboardLimit = leaderboardLimit;
    }

    @Override
    @Transactional
    public MegaTestAttemptResponse submitAttempt(SubmitMegaTestAttemptRequest request,
                                                Long authenticatedUserId) {
        long userId = parsePositiveId(request.getUserId(), "userId");
        long testId = parsePositiveId(request.getTestId(), "testId");
        if (authenticatedUserId != null && !authenticatedUserId.equals(userId)) {
            throw new IllegalArgumentException("userId must match the authenticated user.");
        }

        MegaTestDetails testDetails = detailsRepository.findByIdForUpdate(testId)
                .orElseThrow(() -> new IllegalArgumentException("Mega test not found."));
        if (!testDetails.getClassId().equals(request.getClassId().longValue())) {
            throw new IllegalArgumentException("classId does not match the selected Mega test.");
        }
        validateAgainstTest(request, testDetails);

        String normalizedTestId = Long.toString(testId);
        String normalizedUserId = Long.toString(userId);
        var existing = attemptRepository.findByTestIdAndUserId(normalizedTestId, normalizedUserId);
        if (existing.isPresent()) {
            return toResponse(existing.get(), true);
        }

        LocalDateTime submittedAt = LocalDateTime.now();
        long rankedAhead = attemptRepository.countRankedAheadOfNewAttempt(
                normalizedTestId, request.getScore(), request.getTimeTakenSeconds(), submittedAt);
        int participantsAtSubmission = Math.toIntExact(attemptRepository.countByTestId(normalizedTestId) + 1);
        int rank = Math.toIntExact(rankedAhead + 1);
        BigDecimal percentile = BigDecimal.valueOf(participantsAtSubmission - rank)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(participantsAtSubmission), 1, RoundingMode.DOWN);

        MegaTestAttempt attempt = MegaTestAttempt.builder()
                .testId(normalizedTestId)
                .userId(normalizedUserId)
                .userName(request.getUserName().trim())
                .classId(request.getClassId())
                .score(request.getScore())
                .totalQuestions(request.getTotalQuestions())
                .correctCount(request.getCorrectCount())
                .wrongCount(request.getWrongCount())
                .skippedCount(request.getSkippedCount())
                .timeTakenSeconds(request.getTimeTakenSeconds())
                .rankAtSubmission(rank)
                .participantsAtSubmission(participantsAtSubmission)
                .percentileAtSubmission(percentile)
                .createdAt(submittedAt)
                .build();

        return toResponse(attemptRepository.saveAndFlush(attempt), false);
    }

    @Override
    @Transactional(readOnly = true)
    public MegaTestLeaderboardResponse getLeaderboard(String requestedTestId, Integer classId,
                                                      String requestedUserId) {
        long parsedTestId = parsePositiveId(requestedTestId, "testId");

        String testId = Long.toString(parsedTestId);
        MegaTestDetails testDetails = detailsRepository.findById(parsedTestId)
                .orElseThrow(() -> new IllegalArgumentException("Mega test not found."));
        return buildLeaderboard(testDetails, classId, requestedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public MegaTestLeaderboardResponse getCurrentLeaderboard(Long classId, String requestedUserId) {
        if (classId == null || classId <= 0) {
            throw new IllegalArgumentException("A valid class ID is required.");
        }

        MegaTestDetails testDetails = detailsRepository.findFirstByClassIdOrderByTestDateDescIdDesc(classId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No Mega test is available for this class."));
        return buildLeaderboard(testDetails, classId.intValue(), requestedUserId);
    }

    private MegaTestLeaderboardResponse buildLeaderboard(MegaTestDetails testDetails, Integer classId,
                                                        String requestedUserId) {
        if (classId != null && !testDetails.getClassId().equals(classId.longValue())) {
            throw new IllegalArgumentException("classId does not match the selected Mega test.");
        }

        String testId = Long.toString(testDetails.getId());
        CustomTestPreviewResponse savedTest = readSavedTest(testDetails.getResponseJson());
        long totalParticipants = attemptRepository.countByTestId(testId);
        List<MegaTestAttempt> topAttempts = attemptRepository
                .findByTestIdOrderByScoreDescTimeTakenSecondsAscCreatedAtAscIdAsc(
                        testId, PageRequest.of(0, leaderboardLimit));
        List<MegaTestLeaderboardEntry> leaderboard = IntStream.range(0, topAttempts.size())
                .mapToObj(index -> toLeaderboardEntry(topAttempts.get(index), index + 1))
                .toList();

        MegaTestStudentEntry myEntry = Optional.ofNullable(requestedUserId)
                .filter(userId -> !userId.isBlank())
                .map(userId -> {
                    String normalizedUserId = Long.toString(parsePositiveId(userId, "userId"));
                    return attemptRepository.findByTestIdAndUserId(testId, normalizedUserId)
                            .map(attempt -> toStudentEntry(attempt, calculateRank(attempt), totalParticipants))
                            .orElse(null);
                })
                .orElse(null);

        return MegaTestLeaderboardResponse.builder()
                .success(true)
                .testId(testId)
                .testTitle(savedTest.getTitle())
                .classId(testDetails.getClassId())
                .totalParticipants(totalParticipants)
                .myEntry(myEntry)
                .leaderboard(leaderboard)
                .build();
    }

    private int calculateRank(MegaTestAttempt attempt) {
        return Math.toIntExact(attemptRepository.countRankedAhead(
                attempt.getTestId(),
                attempt.getScore(),
                attempt.getTimeTakenSeconds(),
                attempt.getCreatedAt(),
                attempt.getId()) + 1);
    }

    private MegaTestLeaderboardEntry toLeaderboardEntry(MegaTestAttempt attempt, int rank) {
        return MegaTestLeaderboardEntry.builder()
                .rank(rank)
                .userId(attempt.getUserId())
                .userName(attempt.getUserName())
                .score(attempt.getScore())
                .totalQuestions(attempt.getTotalQuestions())
                .timeTakenSeconds(attempt.getTimeTakenSeconds())
                .build();
    }

    private MegaTestStudentEntry toStudentEntry(MegaTestAttempt attempt, int rank, long totalParticipants) {
        BigDecimal percentile = BigDecimal.valueOf(totalParticipants - rank)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalParticipants), 1, RoundingMode.HALF_UP);
        return MegaTestStudentEntry.builder()
                .rank(rank)
                .userId(attempt.getUserId())
                .userName(attempt.getUserName())
                .score(attempt.getScore())
                .totalQuestions(attempt.getTotalQuestions())
                .timeTakenSeconds(attempt.getTimeTakenSeconds())
                .percentile(percentile)
                .build();
    }

    private CustomTestPreviewResponse readSavedTest(String responseJson) {
        try {
            return objectMapper.readValue(responseJson, CustomTestPreviewResponse.class);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Could not read the saved Mega test.", ex);
        }
    }

    private void validateAgainstTest(SubmitMegaTestAttemptRequest request, MegaTestDetails testDetails) {
        if (request.getScore() > request.getTotalQuestions()) {
            throw new IllegalArgumentException("score cannot exceed totalQuestions.");
        }
        if (request.getCorrectCount() + request.getWrongCount() + request.getSkippedCount()
                != request.getTotalQuestions()) {
            throw new IllegalArgumentException(
                    "correctCount, wrongCount, and skippedCount must add up to totalQuestions.");
        }

        CustomTestPreviewResponse savedTest = readSavedTest(testDetails.getResponseJson());
        if (savedTest.getTotalQuestions() != request.getTotalQuestions()) {
            throw new IllegalArgumentException("totalQuestions does not match the selected Mega test.");
        }
    }

    private long parsePositiveId(String value, String fieldName) {
        try {
            long id = Long.parseLong(value.trim());
            if (id <= 0) {
                throw new NumberFormatException("ID must be positive.");
            }
            return id;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a positive numeric ID.");
        }
    }

    private MegaTestAttemptResponse toResponse(MegaTestAttempt attempt, boolean alreadySubmitted) {
        return MegaTestAttemptResponse.builder()
                .success(true)
                .alreadySubmitted(alreadySubmitted)
                .message(alreadySubmitted ? "Attempt already submitted" : "Attempt submitted successfully")
                .rank(attempt.getRankAtSubmission())
                .totalParticipants(attempt.getParticipantsAtSubmission())
                .percentile(attempt.getPercentileAtSubmission())
                .score(attempt.getScore())
                .totalQuestions(attempt.getTotalQuestions())
                .timeTakenSeconds(attempt.getTimeTakenSeconds())
                .build();
    }
}
