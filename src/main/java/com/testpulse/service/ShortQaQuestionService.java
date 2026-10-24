package com.testpulse.service;

import com.testpulse.dto.ShortQaQuestionGroupResponse;
import com.testpulse.dto.ShortQaQuestionRequest;
import com.testpulse.dto.ShortQaQuestionResponse;
import com.testpulse.model.ShortQaQuestion;

import java.util.List;

public interface ShortQaQuestionService {
    ShortQaQuestion create(ShortQaQuestionRequest request);

    ShortQaQuestion update(Long questionId, ShortQaQuestionRequest request);

    List<ShortQaQuestionGroupResponse> getByClassId(Long classId, String lang);

    ShortQaQuestionGroupResponse getByShortQaId(Long shortQaId, Long classId, String lang);
}