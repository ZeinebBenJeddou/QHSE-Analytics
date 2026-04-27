package com.QHSEAnalytics.entity;

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

    /** Logical group name for this mapping set */
    @Column(name = "template_name", nullable = false, length = 120)
    private String templateName;

    /** The raw column header found in the Excel/CSV file */
    @Column(name = "excel_column", nullable = false, length = 255)
    private String excelColumn;

    /** The KPI this column maps to (nullable – not every column maps to a KPI) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kpi_id")
    private Kpi kpi;

    /** Optional category code hint */
    @Column(name = "categorie_code", length = 50)
    private String categorieCode;

    /** Owner of this mapping */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
