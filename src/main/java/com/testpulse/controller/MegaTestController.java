package com.testpulse.controller;

import com.testpulse.dto.MegaTestResponse;
import com.testpulse.service.MegaTestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tests/mega")
public class MegaTestController {

    private final MegaTestService megaTestService;

    public MegaTestController(MegaTestService megaTestService) {
        this.megaTestService = megaTestService;
    }

    @GetMapping("/current")
    public ResponseEntity<MegaTestResponse> getCurrentMegaTest(
            @RequestParam Long classId,
            @RequestParam(defaultValue = "en") String lang) {
        return ResponseEntity.ok(megaTestService.getCurrentMegaTest(classId,lang));
    }
}
