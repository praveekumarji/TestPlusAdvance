package com.testpulse.service.impl;

import com.testpulse.dto.ShortQaRequest;
import com.testpulse.dto.ShortQaResponse;
import com.testpulse.model.ShortQa;
import com.testpulse.repository.ShortQaRepository;
import com.testpulse.service.ShortQaService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShortQaServiceImpl implements ShortQaService {

    private final ShortQaRepository shortQaRepository;

    public ShortQaServiceImpl(ShortQaRepository shortQaRepository) {
        this.shortQaRepository = shortQaRepository;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"shortQa", "shortQaQuestions"}, allEntries = true)
    public List<ShortQa> createAll(List<ShortQaRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one short-QA item is required.");
        }
        return shortQaRepository.saveAll(requests.stream().map(this::toEntity).toList());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"shortQa", "shortQaQuestions"}, allEntries = true)
    public ShortQa update(Long id, ShortQaRequest request) {
        ShortQa existing = shortQaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Short-QA item not found."));
        copyRequest(existing, request);
        return shortQaRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shortQa", key = "#classId + ':' + #lang")
    public List<ShortQaResponse> getByClassId(Long classId, String lang) {
        boolean hindi = "hi".equalsIgnoreCase(lang);
        return shortQaRepository.findByClassIdOrderByIdAsc(classId).stream()
                .map(item -> new ShortQaResponse(
                        item.getId(),
                        hindi ? item.getSubjectHi() : item.getSubject(),
                        item.getTotalQuestions(),
                        item.getClassId(),
                        item.getTotalTopics(),
                        hindi && item.getTopicsHi() != null && !item.getTopicsHi().isEmpty()
                                ? item.getTopicsHi()
                                : item.getTopics()))
                .toList();
    }

    private ShortQa toEntity(ShortQaRequest request) {
        ShortQa item = new ShortQa();
        copyRequest(item, request);
        return item;
    }

    private void copyRequest(ShortQa item, ShortQaRequest request) {
        if (request == null
                || request.subject() == null || request.subject().isBlank()
                || request.subjectHi() == null || request.subjectHi().isBlank()
                || request.classId() == null
                || request.totalQuestions() < 0
                || request.totalTopics() < 0
                || request.topics() == null
                || request.topicsHi() == null) {
            throw new IllegalArgumentException("Short-QA payload is incomplete or invalid.");
        }
        item.setSubject(request.subject());
        item.setSubjectHi(request.subjectHi());
        item.setTotalQuestions(request.totalQuestions());
        item.setClassId(request.classId());
        item.setTotalTopics(request.totalTopics());
        item.setTopics(List.copyOf(request.topics()));
        item.setTopicsHi(List.copyOf(request.topicsHi()));
    }
}