package com.testpulse.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "short_qa")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortQa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String subject;

    @Column(name = "subject_hi", nullable = false)
    private String subjectHi;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "total_topics", nullable = false)
    private int totalTopics;

    @ElementCollection
    @CollectionTable(name = "short_qa_topics", joinColumns = @JoinColumn(name = "short_qa_id"))
    @Column(name = "topic", nullable = false)
    @OrderColumn(name = "topic_order")
    @Builder.Default
    private List<String> topics = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "short_qa_topics_hi", joinColumns = @JoinColumn(name = "short_qa_id"))
    @Column(name = "topic", nullable = false)
    @OrderColumn(name = "topic_order")
    @Builder.Default
    private List<String> topicsHi = new ArrayList<>();
}