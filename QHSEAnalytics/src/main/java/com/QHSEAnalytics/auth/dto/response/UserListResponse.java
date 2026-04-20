package com.QHSEAnalytics.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserListResponse {
    private List<UserResponse> users;
    private int totalAdmins;
    private int totalAnalystes;
    private int totalActifs;
    private int totalInactifs;
}