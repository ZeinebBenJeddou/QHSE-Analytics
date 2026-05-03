CREATE TABLE IF NOT EXISTS rag_knowledge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    kpi_name VARCHAR(255) NOT NULL UNIQUE,
    definition TEXT,
    thresholds JSON,
    category VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_kpi_name (kpi_name),
    INDEX idx_category (category)
);