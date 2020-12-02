package com.neu.cloudwebapp.question_answer;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.cloudwebapp.response.CustomResponse;
import com.neu.cloudwebapp.user.User;
import com.neu.cloudwebapp.user.UserRepository;
import com.neu.cloudwebapp.user.UserService;
import com.timgroup.statsd.StatsDClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
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

    @Autowired
    private StatsDClient statsDClient;

    private final static Logger LOGGER = LoggerFactory.getLogger(QuesAnsController.class);

    @Autowired
    private AmazonSNS amazonSNS;

    @Value("${webapp.domain:#{null}}")
    private String webappDomain;
    @Value("${sns.topic.arn:#{null}}")
    private String snsTopicArn;

    @GetMapping("/v1/question/{squestion_id}")
    public ResponseEntity<HashMap<String, Object>> getQuestionById(@PathVariable String squestion_id) {

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.question.question_id.api.get");
        LOGGER.info("Fetching question by ID");

        if(!userService.checkUuid(squestion_id)) {
            LOGGER.error("Wrong ID");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.question.http.get", time);
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ), HttpStatus.BAD_REQUEST);
        }

        UUID uuid = UUID.fromString(squestion_id);

        long startdb = System.currentTimeMillis();
        Optional<Question> question= questionRepository.findById(uuid);

        if(!question.isPresent()) {
            LOGGER.error("Question not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.question.http.get", time);
            statsDClient.recordExecutionTime("dbquery.get.question", end - startdb);
            return new ResponseEntity(new CustomResponse(new Date(),"Question not found","" ),HttpStatus.NOT_FOUND);
        }

        LOGGER.info("Question fetched successfully!");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("dbquery.get.question", (System.currentTimeMillis() - startdb));
        statsDClient.recordExecutionTime("timer.question.http.get", time);
        return new ResponseEntity<>(quesAnsService.getQuestion(uuid, 0), HttpStatus.OK);
    }

    @PostMapping("/v1/question/")
    public ResponseEntity<HashMap<String, Object>> postQuestion(@RequestBody Question question, Principal principal) {

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.question.api.post");
        LOGGER.info("Adding new question");

        question.setCreated_timestamp(new Date());
        question.setUpdated_timestamp(new Date());

        long startdb = System.currentTimeMillis();
        User user = userRepository.findUserByUsername(principal.getName());

        if(question.getQuestion_text() == null) {
            LOGGER.error("No question text");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.question.http.post", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Question text mandatory!","" ), HttpStatus.BAD_REQUEST);
        }

        if(user == null) {
            LOGGER.error("User not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.question.http.post", time);
            statsDClient.recordExecutionTime("dbquery.post.question", end - startdb);
            return new ResponseEntity(new CustomResponse(new Date(),"User does not exist!","" ), HttpStatus.BAD_REQUEST);
        }

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

        LOGGER.info("Question added successfully!");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("dbquery.post.question", (System.currentTimeMillis() - startdb));
        statsDClient.recordExecutionTime("timer.question.http.post", time);
        return new ResponseEntity<>(quesAnsService.getQuestion(question.getQuestion_id(), 1), HttpStatus.CREATED);
    }

    @PostMapping("/v1/question/{squestion_id}/answer")
    public ResponseEntity<HashMap<String, Object>> answerQuestion(@RequestBody Answer answer, Principal principal, @PathVariable String squestion_id) throws JSONException {

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.question.question_id.answer.api.post");
        LOGGER.info("Adding new answer");

        answer.setCreated_timestamp(new Date());
        answer.setUpdated_timestamp(new Date());

        long startdb = System.currentTimeMillis();
        User user = userRepository.findUserByUsername(principal.getName());

        answer.setQuestion(questionRepository.findById(UUID.fromString(squestion_id)).get());
        answer.setUser(user);

        answerRepository.save(answer);

        LOGGER.info("Answer added successfully!");

        Question question = questionRepository.findById(UUID.fromString(squestion_id)).get();
        String toEmail = question.getUser().getUsername();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("from", "noreply@" + webappDomain);
        jsonObject.put("to", toEmail);
        jsonObject.put("QuestionID", squestion_id);
        jsonObject.put("message", "Your question " + squestion_id + " was just answered!");
        jsonObject.put("URL", "http://" + webappDomain + "/v1/question/" + squestion_id + "/answer");

        LOGGER.info("JSON string created: " + jsonObject.toString());
        LOGGER.info("Publishing the message to SNS...");

        PublishResult publishResult = amazonSNS.publish(new PublishRequest(snsTopicArn, jsonObject.toString()));

        LOGGER.info("SNS message published: " + publishResult.toString());

        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("dbquery.post.answer", (System.currentTimeMillis() - startdb));
        statsDClient.recordExecutionTime("timer.answer.http.post", time);
        return new ResponseEntity<>(quesAnsService.getAnswer(answer.getAnswer_id()), HttpStatus.CREATED);
    }

    @GetMapping("/v1/questions")
    public ResponseEntity<List<Object>> getAllQuestions(){

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.questions.api.get");
        LOGGER.info("Fetching all questions");
        long startdb = System.currentTimeMillis();
        List<Question> questions = questionRepository.findAll();

        LOGGER.info("Questions fetched successfully!");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("dbquery.get.question", (System.currentTimeMillis() - startdb));
        statsDClient.recordExecutionTime("timer.question.http.get", time);
        return new ResponseEntity<>(quesAnsService.getAllQuestions(questions), HttpStatus.OK);

    }

    @GetMapping("/v1/question/{squestion_id}/answer/{sanswer_id}")
    public ResponseEntity<HashMap<String, Object>> getAQuestionsAnswer(@PathVariable("squestion_id") String squestion_id, @PathVariable("sanswer_id") String sanswer_id) {

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.question.question_id.answer.answer_id.api.get");
        LOGGER.info("Fetching answer to question");

        if(!userService.checkUuid(squestion_id)) {
            LOGGER.error("Wrong question ID type");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.answer.http.get", time);
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ), HttpStatus.BAD_REQUEST);
        }

        if(!userService.checkUuid(sanswer_id)) {
            LOGGER.error("Wrong answer ID type");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.answer.http.get", time);
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ), HttpStatus.BAD_REQUEST);
        }

        UUID answer_id = UUID.fromString(sanswer_id);
        UUID question_id = UUID.fromString(squestion_id);

        long startdb = System.currentTimeMillis();
        Optional<Question> question = questionRepository.findById(question_id);
        Optional<Answer> answer = answerRepository.findById(answer_id);

        if(!question.isPresent()) {
            LOGGER.error("Question not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.get.answer", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.answer.http.get", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Question ID","" ), HttpStatus.BAD_REQUEST);
        }

        if(!answer.isPresent()) {
            LOGGER.error("Answer not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.get.answer", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.answer.http.get", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Answer ID","" ), HttpStatus.BAD_REQUEST);
        }

        HashMap<String, Object> map = quesAnsService.getAnswer(answer_id);


        for(Map.Entry entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if(key.equals("question_id") && value.equals(question.get().getQuestion_id())) {
                LOGGER.info("Answer fetched successfully");
                long end = System.currentTimeMillis();
                long time = end-start;
                statsDClient.recordExecutionTime("dbquery.get.answer", (System.currentTimeMillis() - startdb));
                statsDClient.recordExecutionTime("timer.answer.http.get", time);
                return new ResponseEntity<>(map, HttpStatus.OK);
            }

        }

        LOGGER.error("Given answer ID does not belong to given question ID");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("dbquery.get.answer", (System.currentTimeMillis() - startdb));
        statsDClient.recordExecutionTime("timer.answer.http.get", time);
        return new ResponseEntity(new CustomResponse(new Date(),"Invalid Answer ID for given Question ID","" ), HttpStatus.BAD_REQUEST);

    }

    @DeleteMapping("/v1/question/{squestion_id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable String squestion_id, Principal principal) {

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.question.question_id.api.delete");
        LOGGER.info("Deleting question");

        UUID question_id = UUID.fromString(squestion_id);
        if(!userService.checkUuid(squestion_id)) {
            LOGGER.error("Wrong ID type");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.question.http.delete", time);
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ),HttpStatus.BAD_REQUEST);
        }

        long startdb = System.currentTimeMillis();
        Optional<Question> question = questionRepository.findById(question_id);

        if(!question.isPresent()) {
            LOGGER.error("Question not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.delete.question", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.question.http.delete", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Question not found","" ),HttpStatus.NOT_FOUND);
        }


        if(!question.get().getUser().getUsername().equals(principal.getName())) {
            LOGGER.error("Unauthorized user");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.delete.question", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.question.http.delete", time);
            return new ResponseEntity(new CustomResponse(new Date(),"You are not authorized!","" ),HttpStatus.UNAUTHORIZED);
        }

        if(question.get().getAnswers().size() > 0) {
            LOGGER.error("Cannot delete selected question because it is answered");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.delete.question", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.question.http.delete", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Cannot delete selected question because it is answered","" ),HttpStatus.BAD_REQUEST);

        }

        questionRepository.deleteById(question_id);

        LOGGER.info("Question deleted successfully");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("dbquery.delete.question", (System.currentTimeMillis() - startdb));
        statsDClient.recordExecutionTime("timer.question.http.delete", time);
        return ResponseEntity.noContent().build();

    }

    @DeleteMapping("/v1/question/{squestion_id}/answer/{sanswer_id}")
    public ResponseEntity<Void> deleteAnswer(@PathVariable("squestion_id") String squestion_id, @PathVariable("sanswer_id") String sanswer_id, Principal principal) throws JSONException {

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.question.question_id.answer.answer_id.api.delete");
        LOGGER.info("Deleting answer");

        UUID question_id = UUID.fromString(squestion_id);
        UUID answer_id = UUID.fromString(sanswer_id);

        if(!userService.checkUuid(squestion_id) || !userService.checkUuid(sanswer_id)) {
            LOGGER.error("Wrong ID type");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.answer.http.delete", time);
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ),HttpStatus.BAD_REQUEST);
        }

        long startdb = System.currentTimeMillis();
        Optional<Question> question = questionRepository.findById(question_id);
        Optional<Answer> answer = answerRepository.findById(answer_id);

        if(!answer.get().getUser().getUsername().equals(principal.getName())) {
            LOGGER.error("Unauthorized user");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.answer.http.delete", time);
            statsDClient.recordExecutionTime("dbquery.delete.answer", end - startdb);
            return new ResponseEntity(new CustomResponse(new Date(),"You are not authorized!","" ),HttpStatus.UNAUTHORIZED);
        }

        if(!question.isPresent()) {
            LOGGER.error("Invalid question ID");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.answer.http.delete", time);
            statsDClient.recordExecutionTime("dbquery.delete.answer", end - startdb);
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Question ID","" ), HttpStatus.BAD_REQUEST);
        }

        if(!answer.isPresent()) {
            LOGGER.error("Invalid answer ID");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.answer.http.delete", time);
            statsDClient.recordExecutionTime("dbquery.delete.answer", end - startdb);
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Answer ID","" ), HttpStatus.BAD_REQUEST);
        }


        HashMap<String, Object> map = quesAnsService.getAnswer(answer_id);

        for(Map.Entry entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if(key.equals("question_id") && value.equals(question.get().getQuestion_id())) {

                String toEmail = question.get().getUser().getUsername();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("from", "noreply@"+webappDomain);
                jsonObject.put("to", toEmail);
                jsonObject.put("QuestionID", squestion_id);
                jsonObject.put("AnswerID", sanswer_id);
                jsonObject.put("message", "Answer " + sanswer_id + " deleted for your question " + squestion_id +"!");
                jsonObject.put("URL", "http://" + webappDomain + "/v1/question/" + squestion_id + "/answer/" + sanswer_id);

                LOGGER.info("JSON string created: " + jsonObject.toString());
                LOGGER.info("Publishing the message to SNS...");

                PublishResult publishResult = amazonSNS.publish(new PublishRequest(snsTopicArn, jsonObject.toString()));

                LOGGER.info("SNS message published: " + publishResult.toString());

                answerRepository.deleteById(answer_id);
                LOGGER.info("Answer deleted successfully");
                long end = System.currentTimeMillis();
                long time = end-start;
                statsDClient.recordExecutionTime("timer.answer.http.delete", time);
                statsDClient.recordExecutionTime("dbquery.delete.answer", end - startdb);
                return ResponseEntity.noContent().build();
            }

        }

        LOGGER.error("Invalid Answer ID for given Question ID");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("timer.answer.http.delete", time);
        statsDClient.recordExecutionTime("dbquery.delete.answer", end - startdb);
        return new ResponseEntity(new CustomResponse(new Date(),"Invalid Answer ID for given Question ID","" ), HttpStatus.BAD_REQUEST);

    }

    @PutMapping("/v1/question/{squestion_id}")
    public ResponseEntity<HashMap<String, Object>> updateQuestion(@RequestBody Map<Object, Object> fields, Principal principal, @PathVariable String squestion_id) {

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.question.question_id.api.put");
        LOGGER.info("Updating question");

        if(!userService.checkUuid(squestion_id)) {
            LOGGER.error("Wrong ID type");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.question.http.put", time);
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ),HttpStatus.BAD_REQUEST);
        }

        UUID question_id = UUID.fromString(squestion_id);

        long startdb = System.currentTimeMillis();
        Optional<Question> ques = questionRepository.findById(question_id);

        if(!ques.isPresent()) {
            LOGGER.error("Question not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.question.http.put", time);
            statsDClient.recordExecutionTime("dbquery.put.question", end - startdb);
            return new ResponseEntity(new CustomResponse(new Date(),"Question not found","" ),HttpStatus.NOT_FOUND);
        }


        Question question = ques.get();

        if(!question.getUser().getUsername().equals(principal.getName())) {
            LOGGER.error("Unauthorized user");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.question.http.put", time);
            statsDClient.recordExecutionTime("dbquery.put.question", end - startdb);
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
            LOGGER.error("Attempting to update details [Username, Account Created/Updated, Email Address, ID] : denied");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.question.http.put", time);
            statsDClient.recordExecutionTime("dbquery.put.question", end - startdb);
            return new ResponseEntity(new CustomResponse(new Date(),"Cannot update details","Username, Account Created/Updated, Email Address, ID" ),HttpStatus.BAD_REQUEST);
        }

        question.setUpdated_timestamp(new Date());
        questionRepository.save(question);

        LOGGER.info("Updated question successfully");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("timer.question.http.put", time);
        statsDClient.recordExecutionTime("dbquery.put.question", end - startdb);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/v1/question/{squestion_id}/answer/{sanswer_id}")
    public ResponseEntity updateAnswer(@RequestBody Map<Object, Object> fields, Principal principal, @PathVariable("squestion_id") String squestion_id, @PathVariable("sanswer_id") String sanswer_id) throws JSONException {

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.question.question_id.answer.answer_id.api.post");
        LOGGER.info("Updating answer");

        UUID question_id = UUID.fromString(squestion_id);
        UUID answer_id = UUID.fromString(sanswer_id);

        if(!userService.checkUuid(squestion_id) || !userService.checkUuid(sanswer_id))
        {
            LOGGER.error("Wrong ID type");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.answer.http.put", time);
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ),HttpStatus.BAD_REQUEST);
        }

        long startdb = System.currentTimeMillis();
        Question question = questionRepository.findById(question_id).get();
        Answer answer = answerRepository.findById(answer_id).get();

        if(!answer.getUser().getUsername().equals(principal.getName())) {
            LOGGER.error("Unauthorized user");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.answer.http.put", time);
            statsDClient.recordExecutionTime("dbquery.put.answer", end - startdb);
            return new ResponseEntity(new CustomResponse(new Date(),"You are not authorized!","" ),HttpStatus.UNAUTHORIZED);
        }

        if(question == null) {
            LOGGER.error("Question not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.answer.http.put", time);
            statsDClient.recordExecutionTime("dbquery.put.answer", end - startdb);
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Question ID","" ), HttpStatus.BAD_REQUEST);
        }

        if(answer == null) {
            LOGGER.error("Answer not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.answer.http.put", time);
            statsDClient.recordExecutionTime("dbquery.put.answer", end - startdb);
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
                    LOGGER.error("cannot update anything other than answer text");
                    long end = System.currentTimeMillis();
                    long time = end-start;
                    statsDClient.recordExecutionTime("timer.answer.http.put", time);
                    statsDClient.recordExecutionTime("dbquery.put.answer", end - startdb);
                    return new ResponseEntity(new CustomResponse(new Date(),"You can ONLY update answer_text","" ),HttpStatus.BAD_REQUEST);
                }

                answer.setUpdated_timestamp(new Date());
                answerRepository.save(answer);

                String toEmail = question.getUser().getUsername();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("from", "noreply@"+webappDomain);
                jsonObject.put("to", toEmail);
                jsonObject.put("QuestionID", squestion_id);
                jsonObject.put("AnswerID", sanswer_id);
                jsonObject.put("message", "Answer " + sanswer_id + " updated for your question " + squestion_id +"!");
                jsonObject.put("URL", "http://" + webappDomain + "/v1/question/" + squestion_id + "/answer/" + sanswer_id);

                LOGGER.info("JSON string created: " + jsonObject.toString());
                LOGGER.info("Publishing the message to SNS...");

                PublishResult publishResult = amazonSNS.publish(new PublishRequest(snsTopicArn, jsonObject.toString()));

                LOGGER.info("SNS message published: " + publishResult.toString());


                LOGGER.info("Answer updated successfully");
                long end = System.currentTimeMillis();
                long time = end-start;
                statsDClient.recordExecutionTime("timer.answer.http.put", time);
                statsDClient.recordExecutionTime("dbquery.put.answer", end - startdb);
                return ResponseEntity.noContent().build();
            }

        }

        LOGGER.error("Invalid Answer ID for given Question ID");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("timer.answer.http.put", time);
        statsDClient.recordExecutionTime("dbquery.put.answer", end - startdb);
        return new ResponseEntity(new CustomResponse(new Date(),"Invalid Answer ID for given Question ID","" ), HttpStatus.BAD_REQUEST);

    }
}