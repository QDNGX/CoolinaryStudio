package org.example.projectcooking.integration;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.example.projectcooking.service.EmailService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class IntegrationTestConfig {

    @Bean
    @Primary
    EmailService emailService() {
        return new RecordingEmailService();
    }

    public static final class RecordingEmailService implements EmailService {

        private final Map<String, String> loginCodes = new ConcurrentHashMap<>();
        private final List<String> slotCancellationMessages = new CopyOnWriteArrayList<>();

        @Override
        public void sendLoginCode(String email, String code) {
            loginCodes.put(normalize(email), code);
        }

        @Override
        public void sendSlotCancellation(String email, String programTitle, String reason) {
            slotCancellationMessages.add(email + "|" + programTitle + "|" + reason);
        }

        @Override
        public void sendReminder24h(String email, String programTitle, Instant startAt) {
        }

        @Override
        public void sendReminder2h(String email, String programTitle, Instant startAt) {
        }

        public String loginCodeFor(String email) {
            return loginCodes.get(normalize(email));
        }

        public List<String> slotCancellationMessages() {
            return List.copyOf(slotCancellationMessages);
        }

        public void clear() {
            loginCodes.clear();
            slotCancellationMessages.clear();
        }

        private String normalize(String email) {
            return email.toLowerCase();
        }
    }
}
