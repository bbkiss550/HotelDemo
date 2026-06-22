package com.hotel.service;

import com.hotel.model.AuditLog;
import com.hotel.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository auditLogs;

    public AuditService(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    public void record(String action, String detail) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setDetail(detail);
        auditLogs.save(log);
    }
}
