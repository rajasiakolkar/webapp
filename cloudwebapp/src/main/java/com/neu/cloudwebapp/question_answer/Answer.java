package com.neu.cloudwebapp.question_answer;

import com.neu.cloudwebapp.user.User;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;


@Entity
@Table(name = "Answers")
public class Answer {

//    @Autowired
//    User user;

    @Id
    @GeneratedValue(generator="UUID")
    @GenericGenerator(name="UUID", strategy="org.hibernate.id.UUIDGenerator")
    @Column(name="answer_id", updatable = false, nullable = false)
    private UUID answer_id;

    @ManyToOne
    @JoinColumn(name="question_id", nullable=false)
    private Question question;

//    @Column(name="Question_Id", insert="false", update="false")
//    private UUID question_id = question.getQuestion_id();

    @Column(name="Created_Timestamp")
    private Date created_timestamp;

    @Column(name="Updated_Timestamp")
    private Date updated_timestamp;

    @OneToOne
    @JoinColumn(name="user_id", nullable = false)
    private User user;
    //= user.getId();

    @Column(name="Answer_Text")
    private String answer_text;

    public Answer() { }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    //    public User getUser() {
//        return user;
//    }
//
//    public void setUser(User user) {
//        this.user = user;
//    }

    public UUID getAnswer_id() {
        return answer_id;
    }

    public void setAnswer_id(UUID answer_id) {
        this.answer_id = answer_id;
    }

    public Date getCreated_timestamp() {
        return created_timestamp;
    }

    public void setCreated_timestamp(Date created_timestamp) {
        this.created_timestamp = created_timestamp;
    }

    public Date getUpdated_timestamp() {
        return updated_timestamp;
    }

    public void setUpdated_timestamp(Date updated_timestamp) {
        this.updated_timestamp = updated_timestamp;
    }

//    public UUID getUser_id() {
//        return user_id;
//    }
//
//    public void setUser_id(UUID user_id) {
//        this.user_id = user_id;
//    }

    public String getAnswer_text() {
        return answer_text;
    }

    public void setAnswer_text(String answer_text) {
        this.answer_text = answer_text;
    }
}
