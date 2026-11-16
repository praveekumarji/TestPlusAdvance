package com.testpulse.controller;

import com.testpulse.dto.MegaTestResponse;
import com.testpulse.service.MegaTestNotFoundException;
import com.testpulse.service.MegaTestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tests/mega")
public class MegaTestController {

    private final MegaTestService megaTestService;

    public MegaTestController(MegaTestService megaTestService) {
        this.megaTestService = megaTestService;
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentMegaTest(
            @RequestParam Long classId,
            @RequestParam(defaultValue = "en") String lang) {
        try {
            return ResponseEntity.ok(megaTestService.getCurrentMegaTest(classId, lang));
        } catch (MegaTestNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCurrentMegaTest(
            @RequestParam Long classId,
            @RequestParam String subject,
            @RequestParam int questionCount,
            @RequestParam String difficulty,
            @RequestParam String mode,
            @RequestParam String testName) {
        try {
            return ResponseEntity.ok(megaTestService.createCurrentMegaTest(
                    classId, subject, questionCount, difficulty, mode, testName));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
