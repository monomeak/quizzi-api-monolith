package com.sokmeak.quizapp.modules.attempt.entities;


import com.sokmeak.quizapp.modules.quiz.entity.Quiz;
import com.sokmeak.quizapp.modules.user.entity.User;
import com.sokmeak.quizapp.utils.AttemptStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** One person's run at one quiz. The unique constraint (quiz_id, user_id) lives in the migration. */

@Entity
@Table(name = "quiz_attempt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name  = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name  ="user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;


    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "total_questions")
    private Integer totalQuestions;


    @Column(name = "score_percent")
    private Double scorePercent;


    // one to many quiz attemp answer
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AttemptAnswer> answers = new ArrayList<>();

    /**
     * Sets the back-reference as well as adding to the list. attempt_id is
     * NOT NULL, so an answer added without it fails the cascade insert.
     */
    public void addAnswer(AttemptAnswer attemptAnswer) {
        attemptAnswer.setAttempt(this);
        answers.add(attemptAnswer);
    }

}
