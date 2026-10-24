package com.testpulse.controller;

import com.testpulse.dto.ShortQaRequest;
import com.testpulse.dto.ShortQaResponse;
import com.testpulse.model.ShortQa;
import com.testpulse.service.ShortQaService;
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
@RequestMapping("/api/short-qa")
public class ShortQaController {

    private final ShortQaService shortQaService;

    public ShortQaController(ShortQaService shortQaService) {
        this.shortQaService = shortQaService;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<Map<String, Object>> getByClassId(
            @RequestParam Long classId,
            @RequestParam(defaultValue = "en") String lang) {
        List<ShortQaResponse> data = shortQaService.getByClassId(classId, lang);
        return ResponseEntity.ok(Map.of("status", "success", "data", data));
    }

    @PostMapping({"", "/"})
    public ResponseEntity<?> create(@RequestBody List<ShortQaRequest> requests) {
        try {
            List<ShortQa> created = shortQaService.createAll(requests);
            return ResponseEntity.ok(Map.of("status", "success", "data", created));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", exception.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ShortQaRequest request) {
        try {
            return ResponseEntity.ok(Map.of("status", "success", "data", shortQaService.update(id, request)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", exception.getMessage()));
        }
    }
}