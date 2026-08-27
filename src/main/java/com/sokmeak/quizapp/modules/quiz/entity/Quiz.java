package com.sokmeak.quizapp.modules.quiz.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.sokmeak.quizapp.modules.question.entity.Question;
import com.sokmeak.quizapp.modules.user.entity.User;
import com.sokmeak.quizapp.utils.QuizStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.*;


@Entity
@Table(name = "quiz")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String category;

    /** Short human-friendly code players type to join, e.g. "K4P2QX". */
    @Column(name = "join_code", nullable = false, unique = true, length = 10)
    private String joinCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * cascade = ALL + orphanRemoval: questions are part of the quiz and have no life
     * of their own. Deleting the quiz deletes them; removing one from this list
     * deletes that row.
     */
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    /** Keeps both sides of the relationship consistent - always add through here. */
    public void addQuestion(Question question) {
        question.setQuiz(this);
        question.setPosition(questions.size() + 1);
        questions.add(question);
    }
}

