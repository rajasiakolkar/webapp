package com.neu.cloudwebapp.question_answer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query(value = "SELECT * FROM Category c WHERE c.category = :name", nativeQuery = true)
    Category findByCategoryName(@Param("name") String name);

}
