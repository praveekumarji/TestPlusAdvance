package com.testpulse.service;

import com.testpulse.dto.ShortQaRequest;
import com.testpulse.dto.ShortQaResponse;
import com.testpulse.model.ShortQa;

import java.util.List;

public interface ShortQaService {
    List<ShortQa> createAll(List<ShortQaRequest> requests);

    ShortQa update(Long id, ShortQaRequest request);

    List<ShortQaResponse> getByClassId(Long classId, String lang);
}