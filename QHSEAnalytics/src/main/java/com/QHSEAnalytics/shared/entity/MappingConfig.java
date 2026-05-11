package com.QHSEAnalytics.shared.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mapping_config",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "template_name", "excel_column"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MappingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "template_name", nullable = false, length = 120)
    private String templateName;


    @Column(name = "excel_column", nullable = false, length = 255)
    private String excelColumn;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kpi_id")
    private Kpi kpi;


    @Column(name = "categorie_code", length = 50)
    private String categorieCode;


    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
