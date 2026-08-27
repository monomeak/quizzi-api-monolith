package com.sokmeak.quizapp.modules.attempt.repository;

import com.sokmeak.quizapp.modules.attempt.entities.QuizAttempt;
import com.sokmeak.quizapp.utils.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<QuizAttempt, Long> {
    Optional<QuizAttempt> findByQuizIdAndUserId(Long quizId, Long userId);

    List<QuizAttempt> findAllByUserIdOrderByStartedAtDesc(Long userId);

    /**
     * The leaderboard: best score first, and on a tie the person who finished
     * earlier wins. Derived from the method name - no SQL to get wrong.
     */

    List<QuizAttempt> findTop20ByQuizIdAndStatusOrderByScorePercentDescSubmittedAtAsc(Long quizId, AttemptStatus status);


    @Query("select a from QuizAttempt a left join fetch a.answers where a.id = :id")
    Optional<QuizAttempt> findByIdWithAnswers(@Param("id") Long id); //


}
