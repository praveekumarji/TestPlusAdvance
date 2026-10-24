package com.testpulse.controller;

import com.testpulse.dto.ShortQaQuestionGroupResponse;
import com.testpulse.dto.ShortQaQuestionRequest;
import com.testpulse.model.ShortQaQuestion;
import com.testpulse.service.ShortQaQuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/short-qa/questions")
public class ShortQaQuestionController {

    private final ShortQaQuestionService questionService;

    public ShortQaQuestionController(ShortQaQuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ShortQaQuestionRequest request) {
        try {
            ShortQaQuestion created = questionService.create(request);
            return ResponseEntity.ok(Map.of("status", "success", "id", created.getId()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", exception.getMessage()));
        }
    }

    @PutMapping("/{questionId}")
    public ResponseEntity<?> update(@PathVariable Long questionId,
                                   @RequestBody ShortQaQuestionRequest request) {
        try {
            ShortQaQuestion updated = questionService.update(questionId, request);
            return ResponseEntity.ok(Map.of("status", "success", "id", updated.getId()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", exception.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getQuestions(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long shortQAId,
            @RequestParam(defaultValue = "en") String lang) {
        if (classId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "classId is required."));
        }
        try {
            if (shortQAId != null) {
                ShortQaQuestionGroupResponse group = questionService.getByShortQaId(shortQAId, classId, lang);
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "shortQAId", group.shortQAId(),
                        "subject", group.subject(),
                        "questions", group.questions()));
            }
            List<ShortQaQuestionGroupResponse> data = questionService.getByClassId(classId, lang);
            return ResponseEntity.ok(Map.of("status", "success", "data", data));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", exception.getMessage()));
        }
    }
}