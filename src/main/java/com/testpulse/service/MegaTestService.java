package com.testpulse.service;

import com.testpulse.dto.MegaTestResponse;

public interface MegaTestService {
    MegaTestResponse getCurrentMegaTest(Long classId,String lang);

    MegaTestResponse createCurrentMegaTest(Long classId, String subject, int questionCount,
                                           String difficulty, String mode, String testName,
                                           String testType, String description);
}
