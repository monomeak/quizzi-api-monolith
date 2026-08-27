package com.sokmeak.quizapp.modules.question.controller;

import com.sokmeak.quizapp.modules.question.entity.Question;
import com.sokmeak.quizapp.modules.question.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("questions")
public class QuestionController{
    @Autowired
    QuestionService questionService;
    // request mapping
    @GetMapping("/all")
    public List<Question> allQuestion(){
        return questionService.getAllQuestions();
    }
    @GetMapping("/{id}")
    public Question getQuestionById(@PathVariable Long id){
        return questionService.getQuestionById(id);
    }

}



