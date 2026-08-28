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

    /** How many people have played a quiz - the quiz module asks before allowing a delete. */
    long countByQuizId(Long quizId);

    /**
     * The leaderboard: best score first, and on a tie the person who finished
     * earlier wins. Derived from the method name - no SQL to get wrong.
     */

    List<QuizAttempt> findTop20ByQuizIdAndStatusOrderByScorePercentDescSubmittedAtAsc(Long quizId, AttemptStatus status);


    /**
     * Everything the review screen needs in one round trip: the attempt, its owner,
     * the quiz it belongs to, and every stored answer with the question behind it.
     * Without the fetch joins each answer would lazy-load its own question - the
     * classic N+1 on a 20 question quiz.
     */
    @Query("""
            select distinct a from QuizAttempt a
            join fetch a.user
            join fetch a.quiz
            left join fetch a.answers answer
            left join fetch answer.question
            where a.id = :id
            """)
    Optional<QuizAttempt> findByIdWithAnswers(@Param("id") Long id);


}
