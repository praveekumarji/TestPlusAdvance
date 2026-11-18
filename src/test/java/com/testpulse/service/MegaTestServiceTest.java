package com.testpulse.service;

import com.testpulse.dto.CustomTestPreviewResponse;
import com.testpulse.dto.CustomQuestionResponse;
import com.testpulse.dto.MegaTestResponse;
import com.testpulse.model.EducationClass;
import com.testpulse.model.MegaTestDetails;
import com.testpulse.repository.EducationClassRepository;
import com.testpulse.repository.MegaTestDetailsRepository;
import com.testpulse.service.impl.MegaTestServiceImpl;
import com.testpulse.config.CacheConfig;
import com.testpulse.dto.MegaTestDetailsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MegaTestServiceTest {

    private static final Long CLASS_ID = 1L;
    private static final ZoneId TIME_ZONE = ZoneId.of("Asia/Kolkata");

    private MegaTestDetailsRepository detailsRepository;
    private EducationClassRepository classRepository;
    private CustomTestService customTestService;
    private MegaTestServiceImpl service;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        detailsRepository = mock(MegaTestDetailsRepository.class);
        classRepository = mock(EducationClassRepository.class);
        customTestService = mock(CustomTestService.class);
        cacheManager = new CacheConfig().cacheManager();
        service = new MegaTestServiceImpl(detailsRepository, classRepository, customTestService,
                new ObjectMapper(), cacheManager,
                TIME_ZONE.getId());
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(
                EducationClass.builder().id(CLASS_ID).active(true).build()));
    }

    @Test
    void returnsSavedCurrentWeekTestWithoutRegenerating() {
        LocalDate today = today();
        CustomTestPreviewResponse preview = preview("saved-test");
        MegaTestDetails saved = savedDetails(12L, today, preview);
        when(detailsRepository.findByClassIdAndTestDate(CLASS_ID, today)).thenReturn(Optional.of(saved));

        MegaTestResponse response = service.getCurrentMegaTest(CLASS_ID, "en");

        assertEquals(12L, response.getId());
        assertEquals(CLASS_ID, response.getClassId());
        assertEquals(today, response.getTestDate());
        assertEquals("LIVE", response.getStatus());
        assertEquals("Science PRACTICE Challenge", response.getDetails().getTitle());
        verify(detailsRepository, never()).deleteByClassIdAndTestDateNot(any(), any());
        verify(customTestService, never()).generateCustomTest(any(), any(Integer.class), any(), any(), any(), any());
    }

    @Test
    void cachesResponseForRepeatedRequestsWithSameClassDateAndLanguage() {
        LocalDate today = today();
        MegaTestDetails saved = savedDetails(12L, today, preview("saved-test"));
        when(detailsRepository.findByClassIdAndTestDate(CLASS_ID, today)).thenReturn(Optional.of(saved));

        MegaTestResponse first = service.getCurrentMegaTest(CLASS_ID, "en");
        MegaTestResponse second = service.getCurrentMegaTest(CLASS_ID, "en");

        assertEquals(first, second);
        verify(detailsRepository).findByClassIdAndTestDate(CLASS_ID, today);
    }

    @Test
    void recalculatesStatusOnCachedResponse() {
        LocalDate today = today();
        MegaTestResponse cached = MegaTestResponse.builder()
                .id(12L)
                .classId(CLASS_ID)
                .testDate(today)
                .status("LIVE")
                .details(MegaTestDetailsResponse.builder().title("Cached test").build())
                .build();
        cacheManager.getCache("megaTestResponses")
                .put(CLASS_ID + ":" + today + ":en", cached);

        MegaTestResponse response = service.getCurrentMegaTest(CLASS_ID, "en");

        assertEquals("LIVE", response.getStatus());
        verify(detailsRepository, never()).findByClassIdAndTestDate(any(), any());
    }

    @Test
    void localizesSavedQuestionsForRequestedHindiLanguage() {
        LocalDate today = today();
        CustomQuestionResponse question = CustomQuestionResponse.builder()
                .id(42L)
                .subject("Science")
                .subjectHi("विज्ञान")
                .topic("Plants")
                .topicHi("पौधे")
                .text("What is a plant?")
                .textHi("पौधा क्या है?")
                .options(List.of("Tree", "Rock"))
                .optionsHi(List.of("पेड़", "पत्थर"))
                .explanation("A plant is living.")
                .explanationHi("पौधा सजीव है।")
                .hint("Think of a tree.")
                .hintHi("एक पेड़ के बारे में सोचें।")
                .build();
        MegaTestDetails saved = savedDetails(12L, today,
                preview("saved-test", List.of(question)));
        when(detailsRepository.findByClassIdAndTestDate(CLASS_ID, today)).thenReturn(Optional.of(saved));

        MegaTestResponse response = service.getCurrentMegaTest(CLASS_ID, "hi");
        CustomQuestionResponse localized = response.getDetails().getQuestions().get(0);

        assertEquals("विज्ञान", localized.getSubject());
        assertEquals("पौधे", localized.getTopic());
        assertEquals("पौधा क्या है?", localized.getText());
        assertEquals(42L, localized.getId());
        assertEquals(List.of("पेड़", "पत्थर"), localized.getOptions());
        assertEquals("पौधा सजीव है।", localized.getExplanation());
        assertEquals("एक पेड़ के बारे में सोचें।", localized.getHint());
    }

    @Test
    void createsEnglishSnapshotAndUsesRequestedTestName() {
        LocalDate today = today();
        CustomTestPreviewResponse generated = preview("generated-test", List.of(
                CustomQuestionResponse.builder()
                        .text("Question in English")
                        .textHi("हिंदी में प्रश्न")
                        .options(List.of("English option"))
                        .optionsHi(List.of("हिंदी विकल्प"))
                        .build()));
        when(detailsRepository.findByClassIdAndTestDate(CLASS_ID, today)).thenReturn(Optional.empty());
        when(customTestService.generateCustomTest("science", 10, "MEDIUM", "PRACTICE", "en", CLASS_ID))
                .thenReturn(generated);
        when(detailsRepository.saveAndFlush(any(MegaTestDetails.class))).thenAnswer(invocation -> {
            MegaTestDetails details = invocation.getArgument(0);
            details.setId(15L);
            return details;
        });

        MegaTestResponse response = service.createCurrentMegaTest(
                CLASS_ID, "science", 10, 30, "MEDIUM", "PRACTICE", "Sunday Mega - Science", "PAID",
                "A custom science test");

        assertEquals("Sunday Mega - Science", response.getDetails().getTitle());
        assertEquals("PAID", response.getDetails().getTestType());
        assertEquals("A custom science test", response.getDetails().getDescription());
        assertEquals(30, response.getDetails().getDurationMinutes());
        assertEquals("LIVE", response.getStatus());
        assertEquals("Question in English", response.getDetails().getQuestions().get(0).getText());
        assertEquals(List.of("English option"), response.getDetails().getQuestions().get(0).getOptions());
        verify(customTestService).generateCustomTest("science", 10, "MEDIUM", "PRACTICE", "en", CLASS_ID);
    }

    @Test
    void generatesAndStoresTestWhenCurrentWeekIsMissing() {
        LocalDate today = today();
        CustomTestPreviewResponse generated = preview("generated-test");
        when(detailsRepository.findByClassIdAndTestDate(CLASS_ID, today)).thenReturn(Optional.empty());
        when(customTestService.generateCustomTest("science", 10, "MEDIUM", "PRACTICE", "en", CLASS_ID))
                .thenReturn(generated);
        when(detailsRepository.saveAndFlush(any(MegaTestDetails.class))).thenAnswer(invocation -> {
            MegaTestDetails details = invocation.getArgument(0);
            details.setId(15L);
            return details;
        });

        MegaTestResponse response = service.createCurrentMegaTest(
                CLASS_ID, "science", 10, 25, "MEDIUM", "PRACTICE", "Sunday Mega - Science", "FREE",
                "Daily science test");

        assertEquals(15L, response.getId());
        assertEquals("Sunday Mega - Science", response.getDetails().getTitle());
        assertEquals("FREE", response.getDetails().getTestType());
        assertEquals("Daily science test", response.getDetails().getDescription());
        assertEquals(25, response.getDetails().getDurationMinutes());
        verify(customTestService).generateCustomTest("science", 10, "MEDIUM", "PRACTICE", "en", CLASS_ID);
        verify(detailsRepository).saveAndFlush(any(MegaTestDetails.class));
        verify(detailsRepository).deleteByClassIdAndTestDateNot(CLASS_ID, today);
    }

    @Test
    void concurrentGenerationReturnsTheAlreadySavedWeeklyTest() {
        LocalDate today = today();
        CustomTestPreviewResponse generated = preview("generated-test");
        MegaTestDetails winner = savedDetails(18L, today, preview("winner-test"));
        when(detailsRepository.findByClassIdAndTestDate(CLASS_ID, today))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(customTestService.generateCustomTest("science", 10, "MEDIUM", "PRACTICE", "en", CLASS_ID))
                .thenReturn(generated);
        when(detailsRepository.saveAndFlush(any(MegaTestDetails.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate weekly Mega test"));

        MegaTestResponse response = service.createCurrentMegaTest(
                CLASS_ID, "science", 10, 25, "MEDIUM", "PRACTICE", "Sunday Mega - Science", "FREE",
                "Daily science test");

        assertEquals(18L, response.getId());
        assertEquals("Science PRACTICE Challenge", response.getDetails().getTitle());
        verify(detailsRepository).deleteByClassIdAndTestDateNot(CLASS_ID, today);
    }

    @Test
    void rejectsInactiveOrUnknownClass() {
        when(classRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getCurrentMegaTest(2L, "en"));
        verify(detailsRepository, never()).findByClassIdAndTestDate(any(), any());
    }

    @Test
    void getCurrentMegaTestDoesNotCreateWhenNoSnapshotExists() {
        LocalDate today = today();
        when(detailsRepository.findByClassIdAndTestDate(CLASS_ID, today)).thenReturn(Optional.empty());

        assertThrows(com.testpulse.service.MegaTestNotFoundException.class,
                () -> service.getCurrentMegaTest(CLASS_ID, "en"));
        verify(customTestService, never()).generateCustomTest(any(), any(Integer.class), any(), any(), any(), any());
        verify(detailsRepository, never()).saveAndFlush(any());
        verify(detailsRepository, never()).deleteByClassIdAndTestDateNot(any(), any());
    }

    @Test
    void createReturnsExistingCurrentTestWithoutRegenerating() {
        LocalDate today = today();
        MegaTestDetails existing = savedDetails(12L, today, preview("existing-preview"));
        when(detailsRepository.findByClassIdAndTestDate(CLASS_ID, today)).thenReturn(Optional.of(existing));

        MegaTestResponse response = service.createCurrentMegaTest(
                CLASS_ID, "science", 10, 30, "MEDIUM", "PRACTICE", "New test title", "PAID",
                "New test description");

        assertEquals(12L, response.getId());
        assertEquals("Science PRACTICE Challenge", response.getDetails().getTitle());
        verify(customTestService, never()).generateCustomTest(any(), any(Integer.class), any(), any(), any(), any());
        verify(detailsRepository).deleteByClassIdAndTestDateNot(CLASS_ID, today);
    }

    @Test
    void rejectsUnsupportedTestType() {
        assertThrows(IllegalArgumentException.class, () -> service.createCurrentMegaTest(
                CLASS_ID, "science", 10, 30, "MEDIUM", "PRACTICE", "Sunday Mega", "TRIAL",
                "Daily test"));

        verify(detailsRepository, never()).findByClassIdAndTestDate(any(), any());
        verify(customTestService, never()).generateCustomTest(any(), any(Integer.class), any(), any(), any(), any());
    }

    @Test
    void rejectsBlankDescription() {
        assertThrows(IllegalArgumentException.class, () -> service.createCurrentMegaTest(
                CLASS_ID, "science", 10, 30, "MEDIUM", "PRACTICE", "Daily Mega", "FREE", " "));

        verify(detailsRepository, never()).findByClassIdAndTestDate(any(), any());
        verify(customTestService, never()).generateCustomTest(any(), any(Integer.class), any(), any(), any(), any());
    }

    @Test
    void rejectsNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class, () -> service.createCurrentMegaTest(
                CLASS_ID, "science", 10, 0, "MEDIUM", "PRACTICE", "Daily Mega", "FREE",
                "Daily test"));

        verify(detailsRepository, never()).findByClassIdAndTestDate(any(), any());
        verify(customTestService, never()).generateCustomTest(any(), any(Integer.class), any(), any(), any(), any());
    }

    private static LocalDate today() {
        return LocalDate.now(TIME_ZONE);
    }

    private static CustomTestPreviewResponse preview(String id) {
        return preview(id, List.of());
    }

    private static CustomTestPreviewResponse preview(String id, List<CustomQuestionResponse> questions) {
        return CustomTestPreviewResponse.builder()
                .id(id)
                .title("Science PRACTICE Challenge")
                .subject("science")
                .description("A test")
                .durationMinutes(20)
                .totalQuestions(1)
                .mode("PRACTICE")
                .testType("FREE")
                .questions(questions)
                .build();
    }

    private static MegaTestDetails savedDetails(Long id, LocalDate testDate, CustomTestPreviewResponse preview) {
        try {
            return MegaTestDetails.builder()
                    .id(id)
                    .classId(CLASS_ID)
                    .testDate(testDate)
                    .responseJson(new ObjectMapper().writeValueAsString(preview))
                    .build();
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
