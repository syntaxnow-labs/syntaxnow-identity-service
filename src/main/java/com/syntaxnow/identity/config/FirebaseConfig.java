package com.syntaxnow.identity.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials}")
    private String firebaseCredentialsJson;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {

        log.info("Initializing Firebase using credentials from environment variable");

        if (firebaseCredentialsJson == null || firebaseCredentialsJson.isBlank()) {
            throw new IllegalStateException("FIREBASE_SERVICE_ACCOUNT is not configured");
        }

        try (InputStream serviceAccount =
                     new ByteArrayInputStream(firebaseCredentialsJson.getBytes(StandardCharsets.UTF_8))) {

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            return FirebaseApp.initializeApp(options);
        }
    }
}
