package com.sokmeak.quizapp.modules.question.service;

import com.sokmeak.quizapp.modules.question.entity.Question;
import com.sokmeak.quizapp.modules.question.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {
    @Autowired
    // question repo is  the dao layer: data access object
    QuestionRepository questionRepository;

    public List<Question> getAllQuestions(){
        return questionRepository.findAll();
    }


    public Question getQuestionById(Long id){
        return questionRepository.findById(id).orElseThrow();
    }
    public Question addQuestion(Question question){
        return questionRepository.save(question);
    }

}
