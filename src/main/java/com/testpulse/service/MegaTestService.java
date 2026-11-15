package com.testpulse.service;

import com.testpulse.dto.MegaTestResponse;

public interface MegaTestService {
    MegaTestResponse getCurrentMegaTest(Long classId,String lang);
}
