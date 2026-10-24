package com.testpulse.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "short_qa_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortQaQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "short_qa_id", nullable = false)
    private ShortQa shortQa;

    @Column(name = "subject_en", nullable = false)
    private String subjectEn;

    @Column(name = "subject_hi", nullable = false)
    private String subjectHi;

    @Column(name = "topic_en", nullable = false)
    private String topicEn;

    @Column(name = "topic_hi", nullable = false)
    private String topicHi;

    @Column(name = "question_en", nullable = false, columnDefinition = "TEXT")
    private String questionEn;

    @Column(name = "question_hi", nullable = false, columnDefinition = "TEXT")
    private String questionHi;

    @Column(name = "answer_en", nullable = false, columnDefinition = "TEXT")
    private String answerEn;

    @Column(name = "answer_hi", nullable = false, columnDefinition = "TEXT")
    private String answerHi;

    @Column(name = "remember_trick", columnDefinition = "TEXT")
    private String rememberTrick;

    @Column(name = "remember_trick_hi", columnDefinition = "TEXT")
    private String rememberTrickHi;

    @Column(nullable = false, length = 10)
    private String type;

    @Column(nullable = false)
    private boolean active;

    @ElementCollection
    @CollectionTable(name = "short_qa_question_appear_years", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "appear_year", nullable = false)
    @OrderColumn(name = "year_order")
    @Builder.Default
    private List<Integer> appearYears = new ArrayList<>();
}