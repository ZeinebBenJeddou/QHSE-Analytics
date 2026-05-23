package com.QHSEAnalytics.auth.repository;


import com.QHSEAnalytics.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<User> findByRoleOrderByNomAsc(User.Role role);
    long countByRole(User.Role role);
    long countByActiveTrue();
    long countByActiveFalse();
    long countByRoleAndActiveTrue(User.Role role);

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.nom)    LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.prenom) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.email)  LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY u.createdAt DESC")
    Page<User> search(@Param("q") String query, Pageable pageable);
}