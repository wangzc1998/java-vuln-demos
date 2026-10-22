package com.demo.vuln.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * CMS 文章实体。
 */
@Entity
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue
    private Long id;

    private String title;
    private String category;

    @Column(length = 512)
    private String summary;

    @Column(length = 20000)
    private String content;

    private String author;
    private Integer views;

    private LocalDateTime createdAt;

    public Article() {
    }

    public Article(String title, String category, String summary, String content, String author, Integer views, LocalDateTime createdAt) {
        this.title = title;
        this.category = category;
        this.summary = summary;
        this.content = content;
        this.author = author;
        this.views = views;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }
    public String getAuthor() { return author; }
    public Integer getViews() { return views; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
