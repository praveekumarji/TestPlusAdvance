package com.testpulse.repository;

import com.testpulse.model.ShortQaQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShortQaQuestionRepository extends JpaRepository<ShortQaQuestion, Long> {
    List<ShortQaQuestion> findByShortQa_IdOrderByIdAsc(Long shortQaId);

    List<ShortQaQuestion> findByShortQa_IdInOrderByIdAsc(List<Long> shortQaIds);
}