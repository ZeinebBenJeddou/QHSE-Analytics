package com.QHSEAnalytics.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DualFileProfileResponse {
    private List<ColumnProfileDTO> columnsN1;
    private List<ColumnProfileDTO> columnsN;
}
