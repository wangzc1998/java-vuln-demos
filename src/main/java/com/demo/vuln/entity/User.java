package com.demo.vuln.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * CMS 会员实体。
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private Long id;

    private String username;
    private String department;
    private String role;
    private String email;
    private String password;

    public User() {
    }

    public User(String username, String department, String role, String email, String password) {
        this.username = username;
        this.department = department;
        this.role = role;
        this.email = email;
        this.password = password;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getDepartment() { return department; }
    public String getRole() { return role; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
