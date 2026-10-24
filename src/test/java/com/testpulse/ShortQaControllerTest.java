package com.testpulse;

import com.testpulse.controller.ShortQaController;
import com.testpulse.dto.ShortQaResponse;
import com.testpulse.model.ShortQa;
import com.testpulse.service.ShortQaService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShortQaControllerTest {

    @Test
    void shouldReturnHindiSubjectAndTopicsInSuccessEnvelope() throws Exception {
        ShortQaService service = mock(ShortQaService.class);
        when(service.getByClassId(1L, "hi")).thenReturn(List.of(
                new ShortQaResponse(7L, "विज्ञान", 140, 1L, 2, List.of("जैव प्रक्रम", "प्रकाश"))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ShortQaController(service)).build();

        mockMvc.perform(get("/api/short-qa/").param("classId", "1").param("lang", "hi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].subject").value("विज्ञान"))
                .andExpect(jsonPath("$.data[0].topics[0]").value("जैव प्रक्रम"))
                .andExpect(jsonPath("$.data[0].classId").value(1));
    }

    @Test
    void shouldAcceptBatchCreateRequest() throws Exception {
        ShortQaService service = mock(ShortQaService.class);
        when(service.createAll(anyList())).thenReturn(List.of(ShortQa.builder()
                .id(7L)
                .subject("Science")
                .subjectHi("विज्ञान")
                .totalQuestions(140)
                .classId(1L)
                .totalTopics(2)
                .topics(List.of("Life Processes", "Light"))
                .topicsHi(List.of("जैव प्रक्रम", "प्रकाश"))
                .build()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ShortQaController(service)).build();

        mockMvc.perform(post("/api/short-qa/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"subject\":\"Science\",\"subjectHi\":\"विज्ञान\","
                                + "\"totalQuestions\":140,\"classId\":1,\"totalTopics\":2,"
                                + "\"topics\":[\"Life Processes\",\"Light\"],"
                                + "\"topicsHi\":[\"जैव प्रक्रम\",\"प्रकाश\"]}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(7));

        verify(service).createAll(anyList());
    }
}