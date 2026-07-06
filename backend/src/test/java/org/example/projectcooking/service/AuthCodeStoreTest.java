package org.example.projectcooking.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.example.projectcooking.service.AuthCodeStore.VerifyStatus;
import org.junit.jupiter.api.Test;

class AuthCodeStoreTest {

    private final AuthCodeStore store = new AuthCodeStore();

    @Test
    void verifyReturnsOkAndConsumesCode() {
        Instant now = Instant.parse("2026-07-06T10:00:00Z");
        store.issue("Client@ChefStol.local", "123456", now);

        assertThat(store.verify("client@chefstol.local", "123456", now.plusSeconds(30)))
                .isEqualTo(VerifyStatus.OK);
        assertThat(store.verify("client@chefstol.local", "123456", now.plusSeconds(31)))
                .isEqualTo(VerifyStatus.CODE_EXPIRED);
    }

    @Test
    void invalidCodeIncrementsAttemptsUntilLimit() {
        Instant now = Instant.parse("2026-07-06T10:00:00Z");
        store.issue("client@chefstol.local", "123456", now);

        for (int i = 0; i < AuthCodeStore.MAX_ATTEMPTS - 1; i++) {
            assertThat(store.verify("client@chefstol.local", "000000", now.plusSeconds(i)))
                    .isEqualTo(VerifyStatus.INVALID_CODE);
        }
        assertThat(store.verify("client@chefstol.local", "000000", now.plusSeconds(10)))
                .isEqualTo(VerifyStatus.ATTEMPTS_EXCEEDED);
        assertThat(store.verify("client@chefstol.local", "123456", now.plusSeconds(11)))
                .isEqualTo(VerifyStatus.ATTEMPTS_EXCEEDED);
    }

    @Test
    void expiredCodeIsRemoved() {
        Instant now = Instant.parse("2026-07-06T10:00:00Z");
        store.issue("client@chefstol.local", "123456", now);

        assertThat(store.verify("client@chefstol.local", "123456", now.plus(AuthCodeStore.CODE_TTL).plusSeconds(1)))
                .isEqualTo(VerifyStatus.CODE_EXPIRED);
        assertThat(store.resendWaitSeconds("client@chefstol.local", now.plus(AuthCodeStore.CODE_TTL).plusSeconds(2)))
                .isZero();
    }

    @Test
    void resendWaitSecondsHonorsInterval() {
        Instant now = Instant.parse("2026-07-06T10:00:00Z");
        store.issue("client@chefstol.local", "123456", now);

        assertThat(store.resendWaitSeconds("client@chefstol.local", now.plusSeconds(20))).isEqualTo(40);
        assertThat(store.resendWaitSeconds("client@chefstol.local", now.plusSeconds(60))).isZero();
    }
}
