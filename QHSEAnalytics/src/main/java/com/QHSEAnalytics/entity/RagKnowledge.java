package com.QHSEAnalytics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rag_knowledge")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kpi_name", nullable = false, unique = true)
    private String kpiName;

    @Column(name = "definition", columnDefinition = "TEXT")
    private String definition;

    @Column(name = "thresholds", columnDefinition = "JSON")
    private String thresholds;

    @Column(name = "category")
    private String category;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}