package com.QHSEAnalytics.entity;

import com.QHSEAnalytics.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_mapping_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMappingTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false)
    @Builder.Default
    private Integer ligneEntete = 1;

    @OneToMany(mappedBy = "mappingTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserMappingColonne> colonnes = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
