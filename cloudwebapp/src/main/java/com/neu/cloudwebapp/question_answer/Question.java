package com.neu.cloudwebapp.question_answer;


import com.neu.cloudwebapp.user.User;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "Questions")
public class Question {

    @Id
    @GeneratedValue(generator="UUID")
    @GenericGenerator(name="UUID", strategy="org.hibernate.id.UUIDGenerator")
    @Column(name="question_id", updatable = false, nullable = false)
    private UUID question_id;

    @Column(name="Created_Timestamp")
    private Date created_timestamp;

    @Column(name="Updated_Timestamp")
    private Date updated_timestamp;

    @OneToOne
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @Column(name="Question_Text")
    private String question_text;

    @OneToMany(targetEntity = Category.class, mappedBy = "question")
    @Column(name="Categories")
    private List<Category> categories;

    @OneToMany(targetEntity=Answer.class, mappedBy="question")
    @Column(name = "Answers")
    private List<Answer> answers;

    public Question() { }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getQuestion_text() {
        return question_text;
    }

    public void setQuestion_text(String question_text) {
        this.question_text = question_text;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }

    public UUID getQuestion_id() {
        return question_id;
    }

    public void setQuestion_id(UUID question_id) {
        this.question_id = question_id;
    }
}
