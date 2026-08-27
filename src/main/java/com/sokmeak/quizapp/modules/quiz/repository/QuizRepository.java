package com.sokmeak.quizapp.modules.quiz.repository;

import com.sokmeak.quizapp.modules.quiz.entity.Quiz;
import com.sokmeak.quizapp.utils.QuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Optional<Quiz> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);

    List<Quiz> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<Quiz> findByStatusOrderByCreatedAtDesc(QuizStatus status);

    /**
     * JOIN FETCH loads the quiz AND its questions in one SQL statement.
     * Without it, mapping a list of 20 quizzes fires 21 queries - the classic
     * N+1 problem, and the single most common JPA performance bug.
     */

    @Query("select distinct q from Quiz q left join fetch q.questions where q.id = :id")
    Optional<Quiz> findByIdWithQuestions(Long id);
    // fetch all questions with quiz id







}
