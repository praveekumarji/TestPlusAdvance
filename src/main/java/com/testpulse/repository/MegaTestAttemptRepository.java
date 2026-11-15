package com.testpulse.repository;

import com.testpulse.model.MegaTestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MegaTestAttemptRepository extends JpaRepository<MegaTestAttempt, Long> {

    Optional<MegaTestAttempt> findByTestIdAndUserId(String testId, String userId);

    long countByTestId(String testId);

    List<MegaTestAttempt> findByTestIdOrderByScoreDescTimeTakenSecondsAscCreatedAtAscIdAsc(
            String testId, Pageable pageable);

    @Query("select count(a) from MegaTestAttempt a "
            + "where a.testId = :testId "
            + "and (a.score > :score "
            + "or (a.score = :score and a.timeTakenSeconds < :timeTakenSeconds) "
            + "or (a.score = :score and a.timeTakenSeconds = :timeTakenSeconds "
            + "and (a.createdAt < :createdAt "
            + "or (a.createdAt = :createdAt and a.id < :attemptId))))")
    long countRankedAhead(@Param("testId") String testId,
                          @Param("score") int score,
                          @Param("timeTakenSeconds") int timeTakenSeconds,
                          @Param("createdAt") LocalDateTime createdAt,
                          @Param("attemptId") Long attemptId);

    @Query("select count(a) from MegaTestAttempt a "
            + "where a.testId = :testId "
            + "and (a.score > :score "
            + "or (a.score = :score and a.timeTakenSeconds < :timeTakenSeconds) "
            + "or (a.score = :score and a.timeTakenSeconds = :timeTakenSeconds "
            + "and a.createdAt <= :createdAt))")
    long countRankedAheadOfNewAttempt(@Param("testId") String testId,
                                      @Param("score") int score,
                                      @Param("timeTakenSeconds") int timeTakenSeconds,
                                      @Param("createdAt") LocalDateTime createdAt);
}
