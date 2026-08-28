package com.sokmeak.quizapp.modules.question.entity;

import com.sokmeak.quizapp.modules.quiz.entity.Quiz;
import com.sokmeak.quizapp.utils.DifficultyLevel;
import com.sokmeak.quizapp.utils.OptionKey;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "question")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The owning side of the relationship: this is the table that holds quiz_id. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_text", nullable = false, length = 500)
    private String questionText;

    @Column(name = "option_a", nullable = false)
    private String optionA;

    @Column(name = "option_b", nullable = false)
    private String optionB;

    @Column(name = "option_c")
    private String optionC;

    @Column(name = "option_d")
    private String optionD;

    @Enumerated(EnumType.STRING)
    @Column(name = "correct_option", nullable = false, length = 10)
    private OptionKey correctOption;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 10)
    private DifficultyLevel difficulty;

    /** 1, 2, 3 ... so questions always come back in the order the owner wrote them. */
    @Column(name = "position", nullable = false)
    private Integer position;

}
