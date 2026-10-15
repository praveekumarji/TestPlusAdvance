package com.testpulse.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "referrals", uniqueConstraints = {
        @UniqueConstraint(name = "uk_referrals_referral_id", columnNames = "referral_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("referral_id")
    @Column(name = "referral_id", nullable = false, unique = true, length = 50)
    private String referralId;

    @JsonProperty("teacher_name")
    @Column(name = "teacher_name", nullable = false, length = 150)
    private String teacherName;

    @JsonProperty("agentcode")
    @Column(name = "agentcode", length = 50)
    private String agentCode;

    @JsonProperty("agent_name")
    @Column(name = "agent_name", length = 150)
    private String agentName;

    @JsonProperty("agent_mobile")
    @Column(name = "agent_mobile", length = 30)
    private String agentMobile;

    @JsonProperty("mobile_no")
    @Column(name = "mobile_no", nullable = false, length = 30)
    private String mobileNo;

    @Column(length = 500)
    private String address;

    @JsonProperty("created_timestamp")
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @JsonProperty("updated_timestamp")
    @Column(name = "updated_timestamp", nullable = false)
    private LocalDateTime updatedTimestamp;

    @PrePersist
    void setInitialTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        if (createdTimestamp == null) {
            createdTimestamp = now;
        }
        updatedTimestamp = now;
    }

    @PreUpdate
    void refreshUpdatedTimestamp() {
        updatedTimestamp = LocalDateTime.now();
    }
}