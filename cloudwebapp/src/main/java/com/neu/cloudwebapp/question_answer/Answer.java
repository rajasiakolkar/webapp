package com.neu.cloudwebapp.question_answer;

import com.neu.cloudwebapp.user.User;
import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;
import java.util.Date;
import java.util.UUID;


@Entity
@Table(name = "Answers")
public class Answer {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name="UUID", strategy="org.hibernate.id.UUIDGenerator")
    @Column(name="answer_id", updatable = false, nullable = false)
    private UUID answer_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name="Created_Timestamp")
    private Date created_timestamp;

    @Column(name="Updated_Timestamp")
    private Date updated_timestamp;

    @OneToOne
    @JoinColumn(name="user_id", nullable = false)
    private User user;

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

    public String getAnswer_text() {
        return answer_text;
    }

    public void setAnswer_text(String answer_text) {
        this.answer_text = answer_text;
    }
}
