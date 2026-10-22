package com.demo.vuln.repository;

import com.demo.vuln.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findTop5ByOrderByViewsDesc();
    List<Article> findAllByOrderByCreatedAtDesc();
}
