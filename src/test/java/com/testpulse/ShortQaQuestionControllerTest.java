package com.testpulse;

import com.testpulse.controller.ShortQaQuestionController;
import com.testpulse.dto.ShortQaQuestionGroupResponse;
import com.testpulse.dto.ShortQaQuestionRequest;
import com.testpulse.dto.ShortQaQuestionResponse;
import com.testpulse.model.ShortQaQuestion;
import com.testpulse.service.ShortQaQuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShortQaQuestionControllerTest {

    @Test
    void shouldReturnLocalizedQuestionsForShortQaId() throws Exception {
        ShortQaQuestionService service = mock(ShortQaQuestionService.class);
        when(service.getByShortQaId(1L, 1L, "hi")).thenReturn(new ShortQaQuestionGroupResponse(
                1L,
                "जीव विज्ञान",
                List.of(new ShortQaQuestionResponse(9L, "जैव प्रक्रम", "प्रकाश संश्लेषण क्या है?",
                        "हरे पौधे ग्लूकोज बनाते हैं।", "धूप + पानी + हवा", "PAID", true, List.of(2018, 2021)))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ShortQaQuestionController(service)).build();

        mockMvc.perform(get("/api/short-qa/questions")
                        .param("shortQAId", "1")
                        .param("classId", "1")
                        .param("lang", "hi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.subject").value("जीव विज्ञान"))
                .andExpect(jsonPath("$.questions[0].topic").value("जैव प्रक्रम"))
                .andExpect(jsonPath("$.questions[0].question").value("प्रकाश संश्लेषण क्या है?"));
    }

    @Test
    void shouldCreateAndUpdateQuestion() throws Exception {
        ShortQaQuestionService service = mock(ShortQaQuestionService.class);
        when(service.createAll(anyList())).thenReturn(List.of(
                ShortQaQuestion.builder().id(9L).build(),
                ShortQaQuestion.builder().id(10L).build()));
        when(service.update(org.mockito.ArgumentMatchers.eq(9L), any(ShortQaQuestionRequest.class)))
                .thenReturn(ShortQaQuestion.builder().id(9L).build());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ShortQaQuestionController(service)).build();

        String question = "{\"shortQAId\":1,\"subjectEn\":\"Biology\",\"subjectHi\":\"जीव विज्ञान\","
                + "\"topicEn\":\"Life Processes\",\"topicHi\":\"जैव प्रक्रम\","
                + "\"questionEn\":\"What is photosynthesis?\",\"questionHi\":\"प्रकाश संश्लेषण क्या है?\","
                + "\"answerEn\":\"Plants make glucose.\",\"answerHi\":\"पौधे ग्लूकोज बनाते हैं।\","
                + "\"rememberTrick\":\"SUN + WATER + AIR\",\"rememberTrickHi\":\"धूप + पानी + हवा\","
                + "\"type\":\"PAID\",\"active\":true,\"appearYears\":[2018,2021]}";

        mockMvc.perform(post("/api/short-qa/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + question + "," + question.replace("photosynthesis", "respiration") + "]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.ids[0]").value(9))
                .andExpect(jsonPath("$.ids[1]").value(10));

        mockMvc.perform(put("/api/short-qa/questions/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(question))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));

        verify(service).createAll(anyList());
        verify(service).update(org.mockito.ArgumentMatchers.eq(9L), any(ShortQaQuestionRequest.class));
    }
}