package com.athena.interview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "interview_questions")
@Getter
@Setter
@NoArgsConstructor
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_question_interview"))
    private Interview interview;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "question_text", nullable = false, length = 2000)
    private String questionText;

    @Column(nullable = false)
    private int position;

    public InterviewQuestion(String type, String questionText) {
        this.type = type;
        this.questionText = questionText;
    }
}
