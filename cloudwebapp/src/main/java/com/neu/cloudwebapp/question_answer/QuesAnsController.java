package com.neu.cloudwebapp.question_answer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.cloudwebapp.response.CustomResponse;
import com.neu.cloudwebapp.user.User;
import com.neu.cloudwebapp.user.UserRepository;
import com.neu.cloudwebapp.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Transactional
@RestController
public class QuesAnsController {

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuesAnsService quesAnsService;

    @GetMapping("/v1/question/{squestion_id}")
    public ResponseEntity<HashMap<String, Object>> getQuestionById(@PathVariable String squestion_id) {

        if(!userService.checkUuid(squestion_id)) {
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ), HttpStatus.BAD_REQUEST);
        }

        UUID uuid = UUID.fromString(squestion_id);

        Optional<Question> question= questionRepository.findById(uuid);

        if(!question.isPresent())
            return new ResponseEntity(new CustomResponse(new Date(),"Question not found","" ),HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(quesAnsService.getQuestion(uuid, 0), HttpStatus.OK);
    }

    @PostMapping("/v1/question/")
    public ResponseEntity<HashMap<String, Object>> postQuestion(@RequestBody Question question, Principal principal) {

        question.setCreated_timestamp(new Date());
        question.setUpdated_timestamp(new Date());

        User user = userRepository.findUserByUsername(principal.getName());

        if(question.getQuestion_text() == null)
            return new ResponseEntity(new CustomResponse(new Date(),"Question text mandatory!","" ), HttpStatus.BAD_REQUEST);

        if(user == null)
            return new ResponseEntity(new CustomResponse(new Date(),"User does not exist!","" ), HttpStatus.BAD_REQUEST);

        question.setUser(user);

        List<Category> categories = new ArrayList<>();

        if(question.getCategories() != null) {

            for (Category c : question.getCategories()) {

                c.setCategory(c.getCategory().toLowerCase());
                Category ct = quesAnsService.checkCategory(c.getCategory());

                if(ct != null) {
                    categories.add(ct);
                } else {
                    categories.add(c);
                    categoryRepository.save(c);
                }
            }

            if(!categories.isEmpty()) {
                question.setCategories(categories);
            }

        }

        questionRepository.save(question);

        return new ResponseEntity<>(quesAnsService.getQuestion(question.getQuestion_id(), 1), HttpStatus.CREATED);
    }


    @PostMapping("/v1/question/{squestion_id}/answer")
    public ResponseEntity<HashMap<String, Object>> answerQuestion(@RequestBody Answer answer, Principal principal, @PathVariable String squestion_id) {

        answer.setCreated_timestamp(new Date());
        answer.setUpdated_timestamp(new Date());

        User user = userRepository.findUserByUsername(principal.getName());

        answer.setQuestion(questionRepository.findById(UUID.fromString(squestion_id)).get());
        answer.setUser(user);

        answerRepository.save(answer);

        return new ResponseEntity<>(quesAnsService.getAnswer(answer.getAnswer_id()), HttpStatus.CREATED);

    }

    @GetMapping("/v1/questions")
    public ResponseEntity<List<Object>> getAllQuestions(){

        List<Question> questions = questionRepository.findAll();

        return new ResponseEntity<>(quesAnsService.getAllQuestions(questions), HttpStatus.OK);

    }

    @GetMapping("/v1/question/{squestion_id}/answer/{sanswer_id}")
    public ResponseEntity<HashMap<String, Object>> getAQuestionsAnswer(@PathVariable("squestion_id") String squestion_id, @PathVariable("sanswer_id") String sanswer_id) {

        if(!userService.checkUuid(squestion_id)) {
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ), HttpStatus.BAD_REQUEST);
        }

        if(!userService.checkUuid(sanswer_id)) {
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ), HttpStatus.BAD_REQUEST);
        }

        UUID answer_id = UUID.fromString(sanswer_id);
        UUID question_id = UUID.fromString(squestion_id);

        Optional<Question> question = questionRepository.findById(question_id);
        Optional<Answer> answer = answerRepository.findById(answer_id);

        if(!question.isPresent()) {
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Question ID","" ), HttpStatus.BAD_REQUEST);
        }

        if(!answer.isPresent()) {
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Answer ID","" ), HttpStatus.BAD_REQUEST);
        }

        HashMap<String, Object> map = quesAnsService.getAnswer(answer_id);


        for(Map.Entry entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if(key.equals("question_id") && value.equals(question.get().getQuestion_id())) {
                return new ResponseEntity<>(map, HttpStatus.OK);
            }

        }

        return new ResponseEntity(new CustomResponse(new Date(),"Invalid Answer ID for given Question ID","" ), HttpStatus.BAD_REQUEST);

    }

    @DeleteMapping("/v1/question/{squestion_id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable String squestion_id, Principal principal) {

        UUID question_id = UUID.fromString(squestion_id);
        if(!userService.checkUuid(squestion_id))
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ),HttpStatus.BAD_REQUEST);

        Optional<Question> question = questionRepository.findById(question_id);

        if(!question.isPresent())
            return new ResponseEntity(new CustomResponse(new Date(),"Question not found","" ),HttpStatus.NOT_FOUND);

        if(!question.get().getUser().getUsername().equals(principal.getName())) {
            return new ResponseEntity(new CustomResponse(new Date(),"You are not authorized!","" ),HttpStatus.UNAUTHORIZED);
        }

        if(question.get().getAnswers().size() > 0)
            return new ResponseEntity(new CustomResponse(new Date(),"Cannot delete selected question because it is answered","" ),HttpStatus.BAD_REQUEST);

        questionRepository.deleteById(question_id);
        return ResponseEntity.noContent().build();

    }

    @DeleteMapping("/v1/question/{squestion_id}/answer/{sanswer_id}")
    public ResponseEntity<Void> deleteAnswer(@PathVariable("squestion_id") String squestion_id, @PathVariable("sanswer_id") String sanswer_id, Principal principal) {

        UUID question_id = UUID.fromString(squestion_id);
        UUID answer_id = UUID.fromString(sanswer_id);

        if(!userService.checkUuid(squestion_id) || !userService.checkUuid(sanswer_id))
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ),HttpStatus.BAD_REQUEST);

        Optional<Question> question = questionRepository.findById(question_id);
        Optional<Answer> answer = answerRepository.findById(answer_id);

        if(!answer.get().getUser().getUsername().equals(principal.getName())) {
            return new ResponseEntity(new CustomResponse(new Date(),"You are not authorized!","" ),HttpStatus.UNAUTHORIZED);
        }

        if(!question.isPresent()) {
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Question ID","" ), HttpStatus.BAD_REQUEST);
        }

        if(!answer.isPresent()) {
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Answer ID","" ), HttpStatus.BAD_REQUEST);
        }


        HashMap<String, Object> map = quesAnsService.getAnswer(answer_id);

        for(Map.Entry entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if(key.equals("question_id") && value.equals(question.get().getQuestion_id())) {
                answerRepository.deleteById(answer_id);
                return ResponseEntity.noContent().build();
            }

        }

        return new ResponseEntity(new CustomResponse(new Date(),"Invalid Answer ID for given Question ID","" ), HttpStatus.BAD_REQUEST);

    }

    @PutMapping("/v1/question/{squestion_id}")
    public ResponseEntity<HashMap<String, Object>> updateQuestion(@RequestBody Map<Object, Object> fields, Principal principal, @PathVariable String squestion_id) {

        if(!userService.checkUuid(squestion_id))
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ),HttpStatus.BAD_REQUEST);


        UUID question_id = UUID.fromString(squestion_id);

        Optional<Question> ques = questionRepository.findById(question_id);

        if(!ques.isPresent())
            return new ResponseEntity(new CustomResponse(new Date(),"Question not found","" ),HttpStatus.NOT_FOUND);

        Question question = ques.get();

        if(!question.getUser().getUsername().equals(principal.getName())) {
                return new ResponseEntity(new CustomResponse(new Date(),"You are not authorized!","" ),HttpStatus.UNAUTHORIZED);
        }

        AtomicBoolean flag = new AtomicBoolean(false);

        for (Map.Entry<Object, Object> entry : fields.entrySet()) {
            Object k = entry.getKey();
            Object v = entry.getValue();
            Field field = ReflectionUtils.findField(Question.class, (String) k);
            field.setAccessible(true);
            if (k.equals("question_id")
                    || k.equals("created_timestamp")
                    || k.equals("updated_timestamp")) {
                flag.set(true);
                break;
            }

            if(k.equals("categories")) {
                List<Category> categories = new ArrayList<>();

                ObjectMapper mapper = new ObjectMapper();

                List<Category> categoriesList = mapper.convertValue(
                        v,
                        new TypeReference<List<Category>>(){});

                if(categoriesList.size() == 0) {
                    question.setCategories(categories);
                    ReflectionUtils.setField(field, question, categories);
                } else {

                    for (Category c : categoriesList) {

                        c.setCategory(c.getCategory().toLowerCase());
                        Category ct = quesAnsService.checkCategory(c.getCategory());

                        if(ct != null) {
                            categories.add(ct);
                        } else {
                            categories.add(c);
                            categoryRepository.save(c);
                        }
                    }

                    if(!categories.isEmpty()) {
                        question.setCategories(categories);
                        ReflectionUtils.setField(field, question, categories);
                    }

                }


            }

            if(!k.equals("categories")) {
                ReflectionUtils.setField(field, question, v);
            }

        }

        if(flag.get() == true) {
            return new ResponseEntity(new CustomResponse(new Date(),"Cannot update details","Username, Account Created/Updated, Email Address, ID" ),HttpStatus.BAD_REQUEST);
        }

        question.setUpdated_timestamp(new Date());
        questionRepository.save(question);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/v1/question/{squestion_id}/answer/{sanswer_id}")
    public ResponseEntity updateAnswer(@RequestBody Map<Object, Object> fields, Principal principal, @PathVariable("squestion_id") String squestion_id, @PathVariable("sanswer_id") String sanswer_id) {

        UUID question_id = UUID.fromString(squestion_id);
        UUID answer_id = UUID.fromString(sanswer_id);

        if(!userService.checkUuid(squestion_id) || !userService.checkUuid(sanswer_id))
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ),HttpStatus.BAD_REQUEST);

        Question question = questionRepository.findById(question_id).get();
        Answer answer = answerRepository.findById(answer_id).get();

        if(!answer.getUser().getUsername().equals(principal.getName())) {
            return new ResponseEntity(new CustomResponse(new Date(),"You are not authorized!","" ),HttpStatus.UNAUTHORIZED);
        }

        if(question == null) {
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Question ID","" ), HttpStatus.BAD_REQUEST);
        }

        if(answer == null) {
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Answer ID","" ), HttpStatus.BAD_REQUEST);
        }


        HashMap<String, Object> map = quesAnsService.getAnswer(answer_id);

        for(Map.Entry entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if(key.equals("question_id") && value.equals(question.getQuestion_id())) {


                AtomicBoolean flag = new AtomicBoolean(false);

                for (Map.Entry<Object, Object> entry1 : fields.entrySet()) {
                    Object k = entry1.getKey();
                    Object v = entry1.getValue();
                    Field field = ReflectionUtils.findField(Answer.class, (String) k);
                    field.setAccessible(true);
                    if (k.equals("answer_id")
                            || k.equals("question_id")
                            || k.equals("created_timestamp")
                            || k.equals("updated_timestamp")
                            || k.equals("user_id")) {
                        flag.set(true);
                        break;
                    }

                    ReflectionUtils.setField(field, answer, v);
                }

                if(flag.get() == true) {
                    return new ResponseEntity(new CustomResponse(new Date(),"You can ONLY update answer_text","" ),HttpStatus.BAD_REQUEST);
                }

                answer.setUpdated_timestamp(new Date());
                answerRepository.save(answer);


                return ResponseEntity.noContent().build();
            }

        }

        return new ResponseEntity(new CustomResponse(new Date(),"Invalid Answer ID for given Question ID","" ), HttpStatus.BAD_REQUEST);

    }
}
