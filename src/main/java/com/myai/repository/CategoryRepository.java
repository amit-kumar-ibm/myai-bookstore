package com.myai.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myai.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);
}
