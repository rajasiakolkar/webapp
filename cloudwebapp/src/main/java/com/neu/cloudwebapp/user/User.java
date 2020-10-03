package com.neu.cloudwebapp.user;

import com.neu.cloudwebapp.question_answer.Answer;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(generator="UUID")
    @GenericGenerator(name="UUID", strategy="org.hibernate.id.UUIDGenerator")
    @Column(name="id", updatable = false, nullable = false)
    private UUID user_id;

//    @OneToOne(mappedBy = "User", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
//    private Answer answer;

    @Column(name="Role")
    private String role="USER";

    @Column(name="username")
    String username;

    @Column(name="First_Name")
    String first_name;

    @Column(name="last_Name")
    String last_name;

    @Column(name = "Password")
    String password;

    @Column(name="Email_Address")
    String email_address;

    @Column(name="Account_Created")
    Date account_created;

    @Column(name="Account_Updated")
    Date account_updated;

    public User() {}


    public User(UUID id, String first_name, String last_name, String password, String email_address, Date account_created, Date account_updated) {
        this.user_id = id;
        this.first_name = first_name;
        this.last_name = last_name;
        this.password = password;
        this.email_address = email_address;
        this.account_created = account_created;
        this.account_updated = account_updated;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UUID getId() {
        return user_id;
    }

    public void setId(UUID id) {
        this.user_id = id;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getEmail_address() {
        return email_address;
    }

    public void setEmail_address(String email_address) {
        this.email_address = email_address;
    }

    public Date getAccount_created() {
        return account_created;
    }

    public void setAccount_created(Date account_created) {
        this.account_created = account_created;
    }

    public Date getAccount_updated() {
        return account_updated;
    }

    public void setAccount_updated(Date account_updated) {
        this.account_updated = account_updated;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

//    @Override
//    public String toString() {
//        return getFirst_name() + " " + getLast_name()
//    }
}
