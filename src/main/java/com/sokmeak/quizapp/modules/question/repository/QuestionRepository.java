package com.sokmeak.quizapp.modules.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sokmeak.quizapp.modules.question.entity.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

}
