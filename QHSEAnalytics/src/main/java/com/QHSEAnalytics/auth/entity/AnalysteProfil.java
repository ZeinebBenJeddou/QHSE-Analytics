package com.QHSEAnalytics.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "analyste_profil")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AnalysteProfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "secteur_activite", length = 100)
    private String secteurActivite;

    @Column(name = "taille_site", length = 50)
    private String tailleSite;

    @Column(length = 200)
    private String certifications;

    @Column(name = "objectifs_qhse", length = 500)
    private String objectifsQhse;

    @Column(length = 200)
    private String reglementation;

    @Column(name = "contexte_specifique", length = 500)
    private String contexteSpecifique;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
