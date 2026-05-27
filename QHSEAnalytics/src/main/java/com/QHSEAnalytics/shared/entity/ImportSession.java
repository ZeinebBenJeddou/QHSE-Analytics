package com.QHSEAnalytics.shared.entity;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.shared.enums.ImportMode;
import com.QHSEAnalytics.shared.enums.ImportStatut;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImportMode mode;

    @Column(nullable = false)
    private String nomFichier;

    @Column(length = 20)
    private String templateVersion;

    @Column(name = "file_storage_path")
    private String fileStoragePath;

    @Column(name = "file_storage_bucket", length = 100)
    private String fileStorageBucket;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_checksum", length = 64)
    private String fileChecksum;

    @Column(name = "periode_n1", nullable = false)
    private Integer periodeN1;

    @Column(name = "periode_n", nullable = false)
    private Integer periodeN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImportStatut statut;

    @Column(length = 1000)
    private String messageErreur;

    @Column(name = "contexte_secteur", length = 100)
    private String contexteSecteur;

    @Column(name = "contexte_taille", length = 50)
    private String contexteTaille;

    @Column(name = "contexte_certifications", length = 200)
    private String contexteCertifications;

    @Column(name = "contexte_objectifs", length = 500)
    private String contexteObjectifs;

    @Column(name = "contexte_reglementation", length = 200)
    private String contexteReglementation;

    @Column(name = "contexte_specifique", length = 500)
    private String contexteSpecifique;

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
