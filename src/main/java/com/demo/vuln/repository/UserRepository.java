package com.demo.vuln.repository;

import com.demo.vuln.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}