package com.testpulse.service.impl;

import com.testpulse.dto.ShortQaQuestionGroupResponse;
import com.testpulse.dto.ShortQaQuestionRequest;
import com.testpulse.dto.ShortQaQuestionResponse;
import com.testpulse.model.ShortQa;
import com.testpulse.model.ShortQaQuestion;
import com.testpulse.repository.ShortQaQuestionRepository;
import com.testpulse.repository.ShortQaRepository;
import com.testpulse.service.ShortQaQuestionService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShortQaQuestionServiceImpl implements ShortQaQuestionService {

    private final ShortQaQuestionRepository questionRepository;
    private final ShortQaRepository shortQaRepository;

    public ShortQaQuestionServiceImpl(ShortQaQuestionRepository questionRepository,
                                      ShortQaRepository shortQaRepository) {
        this.questionRepository = questionRepository;
        this.shortQaRepository = shortQaRepository;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "shortQaQuestions", allEntries = true)
    public List<ShortQaQuestion> createAll(List<ShortQaQuestionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one short-QA question is required.");
        }
        List<ShortQaQuestion> questions = requests.stream().map(request -> {
            ShortQaQuestion question = new ShortQaQuestion();
            copyRequest(question, request);
            return question;
        }).toList();
        return questionRepository.saveAll(questions);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "shortQaQuestions", allEntries = true)
    public ShortQaQuestion update(Long questionId, ShortQaQuestionRequest request) {
        ShortQaQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Short-QA question not found."));
        copyRequest(question, request);
        return questionRepository.save(question);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shortQaQuestions", key = "#classId + ':' + #lang")
    public List<ShortQaQuestionGroupResponse> getByClassId(Long classId, String lang) {
        List<ShortQa> shortQas = shortQaRepository.findByClassIdOrderByIdAsc(classId);
        List<Long> shortQaIds = shortQas.stream().map(ShortQa::getId).toList();
        Map<Long, List<ShortQaQuestion>> questionsByShortQaId = shortQaIds.isEmpty()
                ? Map.of()
                : questionRepository.findByShortQa_IdInOrderByIdAsc(shortQaIds).stream()
                        .collect(Collectors.groupingBy(question -> question.getShortQa().getId()));

        return shortQas.stream()
                .map(shortQa -> toGroup(shortQa,
                        questionsByShortQaId.getOrDefault(shortQa.getId(), List.of()), lang))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shortQaQuestions", key = "#shortQaId + ':' + #classId + ':' + #lang")
    public ShortQaQuestionGroupResponse getByShortQaId(Long shortQaId, Long classId, String lang) {
        ShortQa shortQa = shortQaRepository.findById(shortQaId)
                .filter(item -> item.getClassId().equals(classId))
                .orElseThrow(() -> new IllegalArgumentException("Short-QA item not found for this class."));
        List<ShortQaQuestion> questions = questionRepository.findByShortQa_IdOrderByIdAsc(shortQaId);
        return toGroup(shortQa, questions, lang);
    }

    private ShortQaQuestionGroupResponse toGroup(ShortQa shortQa, List<ShortQaQuestion> questions, String lang) {
        boolean hindi = "hi".equalsIgnoreCase(lang);
        return new ShortQaQuestionGroupResponse(
                shortQa.getId(),
                hindi ? shortQa.getSubjectHi() : shortQa.getSubject(),
                questions.stream().map(question -> toResponse(question, hindi)).toList());
    }

    private ShortQaQuestionResponse toResponse(ShortQaQuestion question, boolean hindi) {
        return new ShortQaQuestionResponse(
                question.getId(),
                hindi ? question.getTopicHi() : question.getTopicEn(),
                hindi ? question.getQuestionHi() : question.getQuestionEn(),
                hindi ? question.getAnswerHi() : question.getAnswerEn(),
                hindi ? question.getRememberTrickHi() : question.getRememberTrick(),
                question.getType(),
                question.isActive(),
                question.getAppearYears());
    }

    private void copyRequest(ShortQaQuestion question, ShortQaQuestionRequest request) {
        if (request == null
                || request.shortQAId() == null
                || isBlank(request.subjectEn()) || isBlank(request.subjectHi())
                || isBlank(request.topicEn()) || isBlank(request.topicHi())
                || isBlank(request.questionEn()) || isBlank(request.questionHi())
                || isBlank(request.answerEn()) || isBlank(request.answerHi())
                || request.type() == null
                || request.active() == null
                || request.appearYears() == null) {
            throw new IllegalArgumentException("Short-QA question payload is incomplete or invalid.");
        }
        ShortQa shortQa = shortQaRepository.findById(request.shortQAId())
                .orElseThrow(() -> new IllegalArgumentException("Short-QA item not found."));
        String type = request.type().trim().toUpperCase();
        if (!type.equals("PAID") && !type.equals("FREE")) {
            throw new IllegalArgumentException("Question type must be PAID or FREE.");
        }
        if (request.appearYears().stream().anyMatch(year -> year == null || year < 0)) {
            throw new IllegalArgumentException("Appear years must be non-negative years.");
        }

        question.setShortQa(shortQa);
        question.setSubjectEn(request.subjectEn());
        question.setSubjectHi(request.subjectHi());
        question.setTopicEn(request.topicEn());
        question.setTopicHi(request.topicHi());
        question.setQuestionEn(request.questionEn());
        question.setQuestionHi(request.questionHi());
        question.setAnswerEn(request.answerEn());
        question.setAnswerHi(request.answerHi());
        question.setRememberTrick(request.rememberTrick());
        question.setRememberTrickHi(request.rememberTrickHi());
        question.setType(type);
        question.setActive(request.active());
        question.setAppearYears(List.copyOf(request.appearYears()));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}