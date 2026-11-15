package com.testpulse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "mega_test_details", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mega_test_class_date", columnNames = {"class_id", "test_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MegaTestDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "test_date", nullable = false)
    private LocalDate testDate;

    @Column(name = "response_json", nullable = false, columnDefinition = "LONGTEXT")
    private String responseJson;
}
