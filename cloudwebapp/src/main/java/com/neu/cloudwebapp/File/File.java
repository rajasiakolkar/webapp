package com.neu.cloudwebapp.File;

import com.neu.cloudwebapp.question_answer.Answer;
import com.neu.cloudwebapp.question_answer.Question;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "Files")
public class File {

    @Column(name = "file_name")
    private String fileName;

    @Id
    @GeneratedValue(generator="UUID")
    @GenericGenerator(name="UUID", strategy="org.hibernate.id.UUIDGenerator")
    @Column(name="file_id", updatable = false, nullable = false)
    private UUID file_id;

    @Column(name = "created_date")
    private Date created_date;

    @Column(name = "s3_object_name")
    private String s3_object_name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    private Answer answer;

    public File() {}

    @PrePersist
    protected void onCreate() {
        this.created_date = new Date();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public UUID getFile_id() {
        return file_id;
    }

    public void setFile_id(UUID file_id) {
        this.file_id = file_id;
    }

    public Date getCreated_date() {
        return created_date;
    }

    public void setCreated_date(Date created_date) {
        this.created_date = created_date;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }

    public String getS3_object_name() {
        return s3_object_name;
    }

    public void setS3_object_name(String s3_object_name) {
        this.s3_object_name = s3_object_name;
    }
}
