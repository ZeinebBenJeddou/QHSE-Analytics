package com.QHSEAnalytics.auth.controller;

import com.QHSEAnalytics.auth.dto.request.CreateAnalysteRequest;
import com.QHSEAnalytics.auth.dto.request.UpdateUserRequest;
import com.QHSEAnalytics.auth.dto.response.AuditLogResponse;
import com.QHSEAnalytics.auth.dto.response.MessageResponse;
import com.QHSEAnalytics.auth.dto.response.UserListResponse;
import com.QHSEAnalytics.auth.dto.response.UserResponse;
import com.QHSEAnalytics.auth.service.AdminUserService;
import com.QHSEAnalytics.auth.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AuditLogService  auditLogService;

    @GetMapping("/users")
    public ResponseEntity<UserListResponse> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false)    String search) {
        return ResponseEntity.ok(adminUserService.getAllUsers(
                PageRequest.of(page, size, Sort.by("createdAt").descending()), search));
    }

    @GetMapping("/audit")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLog(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false)    String action,
            @RequestParam(required = false)    String adminEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        boolean hasFilter = (action != null && !action.isBlank())
                || (adminEmail != null && !adminEmail.isBlank())
                || dateDebut != null || dateFin != null;

        if (hasFilter) {
            return ResponseEntity.ok(auditLogService.getFiltered(
                    action, adminEmail, dateDebut, dateFin,
                    PageRequest.of(page, size)));
        }
        return ResponseEntity.ok(auditLogService.getAll(PageRequest.of(page, size)));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createAnalyste(@Valid @RequestBody CreateAnalysteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createAnalyste(request));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(adminUserService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.deleteUser(id));
    }

    @PatchMapping("/users/{id}/verify")
    public ResponseEntity<UserResponse> verifyUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.verifyUser(id));
    }

    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.activateUser(id));
    }

    @PatchMapping("/users/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.deactivateUser(id));
    }

    @PatchMapping("/users/{id}/promote")
    public ResponseEntity<UserResponse> promoteToAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.promoteToAdmin(id));
    }

    @PatchMapping("/users/{id}/demote")
    public ResponseEntity<UserResponse> demoteToAnalyste(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.demoteToAnalyste(id));
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.adminResetPassword(id));
    }
}