package com.neu.cloudwebapp.file;

import com.neu.cloudwebapp.question_answer.*;
import com.neu.cloudwebapp.response.CustomResponse;
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.*;

@RestController
public class FileController {

    @Autowired
    private QuesAnsService quesAnsService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private StatsDClient statsDClient;

    private final static Logger LOGGER = LoggerFactory.getLogger(FileController.class);

    @PostMapping("/v1/question/{question_id}/file")
    public ResponseEntity<?> postQuestionFile(@RequestParam(required = false) MultipartFile file, Principal principal, @PathVariable UUID question_id) throws Exception {
        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.question.question_id.file.api.post");
        LOGGER.info("Adding file to question");

        if(file == null) {
            LOGGER.error("No file selected");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.file.http.post", time);
            return ResponseEntity.badRequest().body(new CustomResponse(new Date(),"Select a file",""));
        }

        //System.out.println(file.getContentType());
        if(!file.getContentType().equals("image/png") && !file.getContentType().equals("image/jpg") && !file.getContentType().equals("image/jpeg")  && !file.getContentType().equals("application/pdf")) {
            LOGGER.error("Wrong file format");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.file.http.post", time);
            return ResponseEntity.badRequest().body(new CustomResponse(new Date(),"Wrong File Format",""));
        }

        long startdb = System.currentTimeMillis();
        Optional<Question> ques = questionRepository.findById(question_id);

        if(!ques.isPresent()) {
            LOGGER.error("Question not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.post.file", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.file.http.post", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Question not found","" ),HttpStatus.NOT_FOUND);
        }

        if(!ques.get().getUser().getUsername().equals(principal.getName())) {
            LOGGER.error("Unauthorized User");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.post.file", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.file.http.post", time);
            return new ResponseEntity(new CustomResponse(new Date(),"You are not authorized!","" ),HttpStatus.UNAUTHORIZED);
        }

        Question question = ques.get();

        List<File> files = question.getAttachments();

        String s3_object_name = question_id + "-" + file.getOriginalFilename();

        File f = new File();
        f.setFileName(file.getOriginalFilename());
        f.setS3_object_name(s3_object_name);

        files.add(f);

        f.setQuestion(question);

        f.setMetadata(fileService.saveFileS3(question_id, file));

        fileRepository.save(f);

        LOGGER.info("File added successfully!");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("timer.s3.post.file", (System.currentTimeMillis() - startdb));
        statsDClient.recordExecutionTime("timer.file.http.post", time);
        return ResponseEntity.ok().body(fileService.getFileData(f.getFile_id()));
    }

    @DeleteMapping("/v1/question/{question_id}/file/{file_id}")
    public ResponseEntity<?> deleteQuestionFile(@PathVariable UUID question_id, @PathVariable UUID file_id, Principal principal) throws Exception {

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.question.question_id.file.file_id.api.delete");
        LOGGER.info("Deleting file attached to question");

        long startdb = System.currentTimeMillis();
        Optional<Question> question = questionRepository.findById(question_id);
        Optional<File> file = fileRepository.findById(file_id);

        if(!question.get().getUser().getUsername().equals(principal.getName())) {
            LOGGER.error("Unauthorized User");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.delete.file", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.file.http.delete", time);
            return new ResponseEntity(new CustomResponse(new Date(),"You are not authorized!","" ),HttpStatus.UNAUTHORIZED);
        }

        if(!question.isPresent()) {
            LOGGER.error("Question not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.delete.file", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.file.http.delete", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Question ID","" ), HttpStatus.BAD_REQUEST);
        }

        if(!file.isPresent()) {
            LOGGER.error("File not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.s3.post.file", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.file.http.delete", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid File ID","" ), HttpStatus.BAD_REQUEST);
        }


        fileRepository.deleteById(file_id);
        fileService.deleteFileS3(file.get().getS3_object_name());
        LOGGER.info("File deleted successfully!");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("timer.s3.post.file", (System.currentTimeMillis() - startdb));
        statsDClient.recordExecutionTime("timer.file.http.delete", time);
        return ResponseEntity.noContent().build();

    }

    @PostMapping("/v1/question/{question_id}/answer/{answer_id}/file")
    public ResponseEntity<?> postAnswerFile(@RequestParam(required = false) MultipartFile file, Principal principal, @PathVariable UUID question_id, @PathVariable UUID answer_id) throws Exception {
        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.question.question_id.answer.answer_id.file.api.post");
        LOGGER.info("Adding file to answer");

        if(file == null) {
            LOGGER.error("No file selected");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.file.http.post", time);
            return ResponseEntity.badRequest().body(new CustomResponse(new Date(),"Select a file",""));
        }

        //System.out.println(file.getContentType());
        if(!file.getContentType().equals("image/png") && !file.getContentType().equals("image/jpg") && !file.getContentType().equals("image/jpeg")  && !file.getContentType().equals("application/pdf")){
            LOGGER.error("Wrong file format");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.file.http.post", time);
            return ResponseEntity.badRequest().body(new CustomResponse(new Date(),"Wrong File Format",""));
        }


        long startdb = System.currentTimeMillis();
        Optional<Question> ques = questionRepository.findById(question_id);
        Optional<Answer> ans = answerRepository.findById(answer_id);

        if(!ques.isPresent()) {
            LOGGER.error("Question not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.post.file", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.file.http.post", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Question not found","" ),HttpStatus.NOT_FOUND);
        }

        if(!ans.isPresent()){
            LOGGER.error("Answer not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.post.file", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.file.http.post", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Answer not found","" ),HttpStatus.NOT_FOUND);
        }

        if(!ans.get().getUser().getUsername().equals(principal.getName())) {
            LOGGER.error("Unauthorized User");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.post.file", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.file.http.post", time);
            return new ResponseEntity(new CustomResponse(new Date(),"You are not authorized!","" ),HttpStatus.UNAUTHORIZED);
        }

        Answer answer = ans.get();

        List<File> files = answer.getAttachments();

        String s3_object_name = answer_id + "-" + file.getOriginalFilename();

        File f = new File();
        f.setFileName(file.getOriginalFilename());
        f.setS3_object_name(s3_object_name);

        files.add(f);

        f.setAnswer(answer);

        f.setMetadata(fileService.saveFileS3(answer_id, file));

        fileRepository.save(f);

        LOGGER.info("File added successfully!");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("timer.s3.post.file", (System.currentTimeMillis() - startdb));
        statsDClient.recordExecutionTime("timer.file.http.post", time);
        return ResponseEntity.ok().body(fileService.getFileData(f.getFile_id()));
    }

    @DeleteMapping("/v1/question/{question_id}/answer/{answer_id}/file/{file_id}")
    public ResponseEntity<?> deleteQuestionFile(@PathVariable UUID question_id, @PathVariable UUID answer_id, @PathVariable UUID file_id, Principal principal) throws Exception {

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.question.question_id.answer.answer_id.file.file_id.api.delete");
        LOGGER.info("Deleting file attached to answer");

        long startdb = System.currentTimeMillis();

        Optional<Question> question = questionRepository.findById(question_id);
        Optional<Answer> answer = answerRepository.findById(answer_id);
        Optional<File> file = fileRepository.findById(file_id);

        if(!answer.get().getUser().getUsername().equals(principal.getName())) {
            LOGGER.error("Unauthorized User");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.delete.file", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.file.http.delete", time);
            return new ResponseEntity(new CustomResponse(new Date(),"You are not authorized!","" ),HttpStatus.UNAUTHORIZED);
        }

        if(!question.isPresent()) {
            LOGGER.error("Question not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.delete.file", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.file.http.delete", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Question ID","" ), HttpStatus.BAD_REQUEST);
        }

        if(!answer.isPresent()) {
            LOGGER.error("Answer not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.delete.file", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.file.http.delete", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Answer ID","" ), HttpStatus.BAD_REQUEST);
        }

        if(!file.isPresent()) {
            LOGGER.error("File not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.delete.file", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.file.http.delete", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid File ID","" ), HttpStatus.BAD_REQUEST);
        }


        fileRepository.deleteById(file_id);

        fileService.deleteFileS3(file.get().getS3_object_name());
        LOGGER.info("File deleted successfully!");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("timer.s3.post.file", (System.currentTimeMillis() - startdb));
        statsDClient.recordExecutionTime("timer.file.http.delete", time);
        return ResponseEntity.noContent().build();

    }

}
