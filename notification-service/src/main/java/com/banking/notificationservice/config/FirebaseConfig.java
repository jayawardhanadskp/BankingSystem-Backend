package com.banking.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Initializes FirebaseApp once at startup if FIREBASE_CREDENTIALS_PATH is
 * configured. If it isn't set (or the file can't be read), Firebase is left
 * uninitialized and a warning is logged - the service must keep running
 * without crashing, since the user may not have set up a Firebase project
 * yet. FcmService checks FirebaseApp.getApps() before every send and no-ops
 * safely when it's empty.
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials-path:}")
    private String credentialsPath;

    @PostConstruct
    public void initializeFirebase() {
        if (!StringUtils.hasText(credentialsPath)) {
            log.warn("FIREBASE_CREDENTIALS_PATH is not set - Firebase Cloud Messaging is disabled. " +
                    "Push notifications will be skipped and logged only.");
            return;
        }

        try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized from credentials at {}", credentialsPath);
            }
        } catch (IOException e) {
            log.warn("Could not initialize Firebase from FIREBASE_CREDENTIALS_PATH={} - " +
                    "push notifications will be skipped and logged only. Cause: {}",
                    credentialsPath, e.getMessage());
        }
    }
}
