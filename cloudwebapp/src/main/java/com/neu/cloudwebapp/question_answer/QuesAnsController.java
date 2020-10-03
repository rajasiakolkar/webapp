package com.neu.cloudwebapp.question_answer;

import com.neu.cloudwebapp.response.CustomResponse;
import com.neu.cloudwebapp.user.User;
import com.neu.cloudwebapp.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@RestController
public class QuesAnsController {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private QuesAnsService quesAnsService;

    @GetMapping("/question/{squestion_id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable String squestion_id) {

        if(!userService.checkUuid(squestion_id)) {
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ), HttpStatus.BAD_REQUEST);
        }

        UUID uuid = UUID.fromString(squestion_id);

        Optional<Question> question= questionRepository.findById(uuid);

        if(!question.isPresent())
            return new ResponseEntity(new CustomResponse(new Date(),"Question not found","" ),HttpStatus.NOT_FOUND);

        return  ResponseEntity.ok(question.get());
    }

}
