package com.banking.notificationservice.service;

import com.banking.notificationservice.client.AccountServiceClient;
import com.banking.notificationservice.dto.AccountSummaryResponse;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcmService {

    private final AccountServiceClient accountServiceClient;

    /**
     * Sends a data-only FCM push (no `notification` block - the client app
     * controls rendering) to the given account's registered device, if any.
     * Never throws: any failure is logged and swallowed so a push failure
     * can never break the Kafka consumer that triggered it.
     */
    public void sendPush(String accountNumber, String type, Map<String, String> data, String title, String body) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase not configured - skipping push notification (type={}, account={})", type, accountNumber);
            return;
        }

        if (accountNumber == null || accountNumber.isBlank()) {
            log.warn("Skipping push notification - no account number provided (type={})", type);
            return;
        }

        String fcmToken;
        try {
            AccountSummaryResponse account = accountServiceClient.getAccount(accountNumber);
            fcmToken = account != null ? account.getFcmToken() : null;
        } catch (Exception e) {
            log.warn("Could not look up FCM token for account {} - skipping push. Cause: {}", accountNumber, e.getMessage());
            return;
        }

        if (!StringUtils.hasText(fcmToken)) {
            log.info("No FCM token registered for account {} - skipping push (type={})", accountNumber, type);
            return;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("title", title);
        payload.put("body", body);
        if (data != null) {
            payload.putAll(data);
        }

        Message message = Message.builder()
                .setToken(fcmToken)
                .putAllData(payload)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("FCM push sent (type={}, account={}, messageId={})", type, accountNumber, messageId);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM push (type={}, account={}): {}", type, accountNumber, e.getMessage());
        }
    }
}
