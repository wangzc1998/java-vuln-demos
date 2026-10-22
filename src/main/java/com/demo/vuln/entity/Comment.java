package com.demo.vuln.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 文章评论实体。
 */
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue
    private Long id;

    private Long articleId;
    private String author;

    @Column(length = 4096)
    private String content;

    private LocalDateTime createdAt;

    public Comment() {
    }

    public Comment(Long articleId, String author, String content, LocalDateTime createdAt) {
        this.articleId = articleId;
        this.author = author;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getArticleId() { return articleId; }
    public String getAuthor() { return author; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
