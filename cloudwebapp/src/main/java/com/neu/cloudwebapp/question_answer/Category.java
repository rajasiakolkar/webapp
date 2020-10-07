package com.neu.cloudwebapp.question_answer;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "Category")
public class Category {

    @Id
    @GeneratedValue(generator="UUID")
    @GenericGenerator(name="UUID", strategy="org.hibernate.id.UUIDGenerator")
    @Column(name="category_id", updatable = false, nullable = false)
    private UUID category_id;

    @Column(name="Category")
    private String category;

    @ManyToMany(mappedBy = "categories", cascade = CascadeType.PERSIST)
    private List<Question> questions;

    public Category() { }

    public UUID getCategory_id() {
        return category_id;
    }

    public void setCategory_id(UUID category_id) {
        this.category_id = category_id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
