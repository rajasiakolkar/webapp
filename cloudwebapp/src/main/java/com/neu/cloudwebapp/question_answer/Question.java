package com.neu.cloudwebapp.question_answer;


import com.neu.cloudwebapp.file.File;
import com.neu.cloudwebapp.user.User;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.*;
import java.util.*;

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

    @Column(name="Question_Text", nullable = false)
    private String question_text;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "question_categories",
    joinColumns = {@JoinColumn(name = "question_id")},
    inverseJoinColumns = {@JoinColumn(name = "category_id")})
    private List<Category> categories;

    @OneToMany(cascade = CascadeType.ALL,  mappedBy="question")
    private List<Answer> answers;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "question")
    private List<File> attachments;

    public Question() { }

    public List<File> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<File> attachments) {
        this.attachments = attachments;
    }

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
