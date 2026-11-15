package com.testpulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MegaTestResponse {
    private Long id;
    private Long classId;
    private LocalDate testDate;
    private String status;
    private MegaTestDetailsResponse details;
}
