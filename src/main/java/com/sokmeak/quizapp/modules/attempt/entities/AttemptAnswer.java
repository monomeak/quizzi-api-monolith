package com.sokmeak.quizapp.modules.attempt.entities;


import com.sokmeak.quizapp.modules.question.entity.Question;
import com.sokmeak.quizapp.utils.OptionKey;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attempt_answer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;


    /** null means the player skipped the question. */
    @Enumerated(EnumType.STRING)
    @Column(name = "selected_option", length = 1)
    private OptionKey selectedOption;

    @Column(nullable = false)
    private boolean correct;


}
