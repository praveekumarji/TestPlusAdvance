package com.testpulse.service.impl;

import com.testpulse.dto.CustomTestPreviewResponse;
import com.testpulse.dto.MegaTestDetailsResponse;
import com.testpulse.dto.MegaTestResponse;
import com.testpulse.model.EducationClass;
import com.testpulse.model.MegaTestDetails;
import com.testpulse.repository.EducationClassRepository;
import com.testpulse.repository.MegaTestDetailsRepository;
import com.testpulse.service.CustomTestService;
import com.testpulse.service.MegaTestService;
import com.testpulse.util.LocalizedTextResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

@Service
public class MegaTestServiceImpl implements MegaTestService {

    private final MegaTestDetailsRepository megaTestDetailsRepository;
    private final EducationClassRepository educationClassRepository;
    private final CustomTestService customTestService;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final String subject;
    private final int questionCount;
    private final String difficulty;
    private final String mode;
    private final ZoneId timeZone;

    public MegaTestServiceImpl(
            MegaTestDetailsRepository megaTestDetailsRepository,
            EducationClassRepository educationClassRepository,
            CustomTestService customTestService,
            ObjectMapper objectMapper,
            CacheManager cacheManager,
            @Value("${mega-test.subject}")
            String subject,
            @Value("${mega-test.question-count}")
            int questionCount,
            @Value("${mega-test.difficulty}")
            String difficulty,
            @Value("${mega-test.mode}")
            String mode,
            @Value("${mega-test.time-zone:Asia/Kolkata}")
            String timeZone
    ) {
        this.megaTestDetailsRepository = megaTestDetailsRepository;
        this.educationClassRepository = educationClassRepository;
        this.customTestService = customTestService;
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
        this.subject = subject;
        this.questionCount = questionCount;
        this.difficulty = difficulty;
        this.mode = mode;
        this.timeZone = ZoneId.of(timeZone);
    }

    @Override
    public MegaTestResponse getCurrentMegaTest(Long classId,String lang) {
        if (classId == null || classId <= 0) {
            throw new IllegalArgumentException("A valid class ID is required.");
        }

        EducationClass educationClass = educationClassRepository.findById(classId)
                .filter(EducationClass::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Active class not found: " + classId));

        LocalDate testDate = LocalDate.now(timeZone)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        String normalizedLang = lang == null || lang.isBlank()
                ? "en" : lang.trim().toLowerCase(Locale.ROOT);
        String cacheKey = classId + ":" + testDate + ":" + normalizedLang;
        Cache cache = cacheManager.getCache("megaTestResponses");
        if (cache == null) {
            throw new IllegalStateException("Mega test response cache is not configured.");
        }
        MegaTestResponse cachedResponse = cache.get(cacheKey, MegaTestResponse.class);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        MegaTestResponse response = megaTestDetailsRepository.findByClassIdAndTestDate(classId, testDate)
                .map(savedDetails -> {
                    deleteOtherWeeks(classId, testDate);
                    return toResponse(savedDetails, normalizedLang);
                })
                .orElseGet(() -> generateAndSave(educationClass.getId(), testDate, normalizedLang));
        cache.put(cacheKey, response);
        return response;
    }

    private MegaTestResponse generateAndSave(Long classId, LocalDate testDate, String lang) {
        CustomTestPreviewResponse generated = customTestService.generateCustomTest(
                subject, questionCount, difficulty, mode, "en", classId);
        MegaTestDetails details = MegaTestDetails.builder()
                .classId(classId)
                .testDate(testDate)
                .responseJson(writeResponse(generated))
                .build();

        try {
            MegaTestDetails savedDetails = megaTestDetailsRepository.saveAndFlush(details);
            deleteOtherWeeks(classId, testDate);
            return toResponse(savedDetails, lang);
        } catch (DataIntegrityViolationException ex) {
            return megaTestDetailsRepository.findByClassIdAndTestDate(classId, testDate)
                    .map(savedDetails -> {
                        deleteOtherWeeks(classId, testDate);
                        return toResponse(savedDetails, lang);
                    })
                    .orElseThrow(() -> ex);
        }
    }

    private void deleteOtherWeeks(Long classId, LocalDate currentTestDate) {
        megaTestDetailsRepository.deleteByClassIdAndTestDateNot(classId, currentTestDate);
    }

    private MegaTestResponse toResponse(MegaTestDetails savedDetails, String lang) {
        CustomTestPreviewResponse preview = localizeResponse(
                readResponse(savedDetails.getResponseJson()), lang);
        return MegaTestResponse.builder()
                .id(savedDetails.getId())
                .classId(savedDetails.getClassId())
                .testDate(savedDetails.getTestDate())
                .status(savedDetails.getTestDate().equals(LocalDate.now(timeZone)) ? "LIVE" : "ENDED")
                .details(toDetailsResponse(preview))
                .build();
    }

    private MegaTestDetailsResponse toDetailsResponse(CustomTestPreviewResponse preview) {
        return MegaTestDetailsResponse.builder()
                .title(preview.getTitle())
                .subject(preview.getSubject())
                .description(preview.getDescription())
                .durationMinutes(preview.getDurationMinutes())
                .totalQuestions(preview.getTotalQuestions())
                .mode(preview.getMode())
                .testType(preview.getTestType())
                .questions(preview.getQuestions())
                .build();
    }

    private CustomTestPreviewResponse localizeResponse(CustomTestPreviewResponse response, String lang) {
        if (response.getQuestions() == null) {
            return response;
        }

        response.setQuestions(response.getQuestions().stream()
                .map(question -> {
                    question.setSubject(LocalizedTextResolver.resolve(
                            question.getSubject(), question.getSubjectHi(), lang));
                    question.setTopic(LocalizedTextResolver.resolve(
                            question.getTopic(), question.getTopicHi(), lang));
                    question.setText(LocalizedTextResolver.resolve(
                            question.getText(), question.getTextHi(), lang));
                    question.setOptions(LocalizedTextResolver.resolveList(
                            question.getOptions(), question.getOptionsHi(), lang));
                    question.setExplanation(LocalizedTextResolver.resolve(
                            question.getExplanation(), question.getExplanationHi(), lang));
                    question.setHint(LocalizedTextResolver.resolve(
                            question.getHint(), question.getHintHi(), lang));
                    return question;
                })
                .toList());
        return response;
    }

    private String writeResponse(CustomTestPreviewResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Could not serialize the generated Mega test.", ex);
        }
    }

    private CustomTestPreviewResponse readResponse(String responseJson) {
        try {
            return objectMapper.readValue(responseJson, CustomTestPreviewResponse.class);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Could not read the saved Mega test.", ex);
        }
    }
}
