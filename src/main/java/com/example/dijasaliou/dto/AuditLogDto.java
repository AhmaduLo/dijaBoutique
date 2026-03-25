package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.AuditLog;

import java.time.LocalDateTime;

public record AuditLogDto(
        Long id,
        String action,
        String details,
        String auteur,
        LocalDateTime dateAction
) {
    public static AuditLogDto fromEntity(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getAction(),
                log.getDetails(),
                log.getAuteur(),
                log.getDateAction()
        );
    }
}
