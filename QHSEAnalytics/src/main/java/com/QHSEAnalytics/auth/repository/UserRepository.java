package com.QHSEAnalytics.auth.repository;


import com.QHSEAnalytics.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByOrderByCreatedAtDesc();
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<User> findByRoleOrderByNomAsc(User.Role role);
    long countByRole(User.Role role);
    long countByActiveTrue();
    long countByActiveFalse();
    long countByRoleAndActiveTrue(User.Role role);
}