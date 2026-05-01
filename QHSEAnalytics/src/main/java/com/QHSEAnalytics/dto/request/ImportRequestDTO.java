package com.QHSEAnalytics.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRequestDTO {
    private MultipartFile file;
    private Integer yearN;
    private Integer yearN1;
    private Map<String, Integer> mappingIndexes;
}
