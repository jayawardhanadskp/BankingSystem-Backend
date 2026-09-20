package com.banking.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Slim, service-local copy of account-service's AccountResponse - only the
 * fields notification-service actually needs. Mirrors the same
 * DTO-duplication pattern already used between services in this codebase
 * (see fraud-detection-service's AccountServiceClient).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummaryResponse {
    private String accountNumber;
    private String fcmToken;
}
