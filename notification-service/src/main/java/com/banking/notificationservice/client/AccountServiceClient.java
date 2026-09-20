package com.banking.notificationservice.client;

import com.banking.notificationservice.dto.AccountSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Calls account-service directly (bypassing the gateway) - same pattern as
 * fraud-detection-service's client. notification-service is a pure Kafka
 * consumer with no caller JWT to forward, so it talks to account-service
 * internally on port 8081.
 */
@FeignClient(name = "account-service", url = "${account.service.url}")
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/{accountNumber}")
    AccountSummaryResponse getAccount(@PathVariable String accountNumber);
}
