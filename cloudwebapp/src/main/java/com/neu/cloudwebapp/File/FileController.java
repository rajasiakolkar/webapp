package com.neu.cloudwebapp.File;

import com.neu.cloudwebapp.question_answer.*;
import com.neu.cloudwebapp.response.CustomResponse;
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

    @PostMapping("/question/{question_id}/file")
    public ResponseEntity<?> postQuestionFile(@RequestParam(required = false) MultipartFile file, Principal principal, @PathVariable UUID question_id) throws Exception {
        if(file == null) return ResponseEntity.badRequest().body(new CustomResponse(new Date(),"Select a file",""));

        System.out.println(file.getContentType());
        if(!file.getContentType().equals("image/png") && !file.getContentType().equals("image/jpg") && !file.getContentType().equals("image/jpeg")  && !file.getContentType().equals("application/pdf"))
            return ResponseEntity.badRequest().body(new CustomResponse(new Date(),"Wrong File Format",""));

        Optional<Question> ques = questionRepository.findById(question_id);

        if(!ques.isPresent())
            return new ResponseEntity(new CustomResponse(new Date(),"Question not found","" ),HttpStatus.NOT_FOUND);

        if(!ques.get().getUser().getUsername().equals(principal.getName())) {
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
        fileRepository.save(f);

        fileService.saveFileS3(question_id, file);

        return ResponseEntity.ok().body(fileService.getFileData(f.getFile_id()));
    }

    @DeleteMapping("/question/{question_id}/file/{file_id}")
    public ResponseEntity<?> deleteQuestionFile(@PathVariable UUID question_id, @PathVariable UUID file_id, Principal principal) throws Exception {

        Optional<Question> question = questionRepository.findById(question_id);
        Optional<File> file = fileRepository.findById(file_id);

        if(!question.get().getUser().getUsername().equals(principal.getName())) {
            return new ResponseEntity(new CustomResponse(new Date(),"You are not authorized!","" ),HttpStatus.UNAUTHORIZED);
        }

        if(!question.isPresent()) {
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Question ID","" ), HttpStatus.BAD_REQUEST);
        }

        if(!file.isPresent()) {
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid File ID","" ), HttpStatus.BAD_REQUEST);
        }


        fileRepository.deleteById(file_id);

        fileService.deleteFileS3(file.get().getS3_object_name());
        return ResponseEntity.noContent().build();

    }

    @PostMapping("/question/{question_id}/answer/{answer_id}/file")
    public ResponseEntity<?> postAnswerFile(@RequestParam(required = false) MultipartFile file, Principal principal, @PathVariable UUID question_id, @PathVariable UUID answer_id) throws Exception {
        if(file == null) return ResponseEntity.badRequest().body(new CustomResponse(new Date(),"Select a file",""));

        System.out.println(file.getContentType());
        if(!file.getContentType().equals("image/png") && !file.getContentType().equals("image/jpg") && !file.getContentType().equals("image/jpeg")  && !file.getContentType().equals("application/pdf"))
            return ResponseEntity.badRequest().body(new CustomResponse(new Date(),"Wrong File Format",""));

        Optional<Question> ques = questionRepository.findById(question_id);
        Optional<Answer> ans = answerRepository.findById(answer_id);

        if(!ques.isPresent())
            return new ResponseEntity(new CustomResponse(new Date(),"Question not found","" ),HttpStatus.NOT_FOUND);

        if(!ans.isPresent())
            return new ResponseEntity(new CustomResponse(new Date(),"Answer not found","" ),HttpStatus.NOT_FOUND);

        if(!ans.get().getUser().getUsername().equals(principal.getName())) {
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
        fileRepository.save(f);

        fileService.saveFileS3(answer_id, file);

        return ResponseEntity.ok().body(fileService.getFileData(f.getFile_id()));
    }

    @DeleteMapping("/question/{question_id}/answer/{answer_id}/file/{file_id}")
    public ResponseEntity<?> deleteQuestionFile(@PathVariable UUID question_id, @PathVariable UUID answer_id, @PathVariable UUID file_id, Principal principal) throws Exception {

        Optional<Question> question = questionRepository.findById(question_id);
        Optional<Answer> answer = answerRepository.findById(answer_id);
        Optional<File> file = fileRepository.findById(file_id);

        if(!answer.get().getUser().getUsername().equals(principal.getName())) {
            return new ResponseEntity(new CustomResponse(new Date(),"You are not authorized!","" ),HttpStatus.UNAUTHORIZED);
        }

        if(!question.isPresent()) {
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Question ID","" ), HttpStatus.BAD_REQUEST);
        }

        if(!answer.isPresent()) {
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid Answer ID","" ), HttpStatus.BAD_REQUEST);
        }

        if(!file.isPresent()) {
            return new ResponseEntity(new CustomResponse(new Date(),"Invalid File ID","" ), HttpStatus.BAD_REQUEST);
        }


        fileRepository.deleteById(file_id);

        fileService.deleteFileS3(file.get().getS3_object_name());
        return ResponseEntity.noContent().build();

    }

}
