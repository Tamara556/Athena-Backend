package com.athena.ai.entity;

import com.athena.ai.domain.QuizType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(columnDefinition = "text", nullable = false)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizType type;

    @Convert(converter = StringListConverter.class)
    @Column(name = "options_json", columnDefinition = "text", nullable = false)
    private List<String> options;

    @Column(name = "correct_answer", nullable = false, length = 1000)
    private String correctAnswer;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public QuizQuestion(UUID sessionId, String question, QuizType type, List<String> options,
                        String correctAnswer, String explanation, int orderIndex) {
        this.sessionId = sessionId;
        this.question = question;
        this.type = type;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.orderIndex = orderIndex;
    }
}
