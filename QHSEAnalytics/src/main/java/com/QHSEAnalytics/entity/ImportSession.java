package com.QHSEAnalytics.entity;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.enums.ImportMode;
import com.QHSEAnalytics.enums.ImportStatut;
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

    @Lob
    @Column(name = "file_content")
    private byte[] fileContent;

    @Column(nullable = false)
    private Integer periodeN1;

    @Column(nullable = false)
    private Integer periodeN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImportStatut statut;

    @Column(length = 1000)
    private String messageErreur;

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
