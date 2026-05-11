package com.QHSEAnalytics.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryRateLimiter — tests unitaires")
class InMemoryRateLimiterTest {

    private InMemoryRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new InMemoryRateLimiter();
        ReflectionTestUtils.setField(rateLimiter, "maxAttempts", 3);
        ReflectionTestUtils.setField(rateLimiter, "windowSeconds", 60L);
    }

    @Test
    @DisplayName("les premières tentatives sont autorisées")
    void allowRequest_withinLimit_returnsTrue() {
        assertThat(rateLimiter.allowRequest("192.168.1.1", "/api/auth/login")).isTrue();
        assertThat(rateLimiter.allowRequest("192.168.1.1", "/api/auth/login")).isTrue();
        assertThat(rateLimiter.allowRequest("192.168.1.1", "/api/auth/login")).isTrue();
    }

    @Test
    @DisplayName("la tentative suivant le seuil est bloquée")
    void allowRequest_exceedsLimit_returnsFalse() {
        rateLimiter.allowRequest("10.0.0.1", "/api/auth/login");
        rateLimiter.allowRequest("10.0.0.1", "/api/auth/login");
        rateLimiter.allowRequest("10.0.0.1", "/api/auth/login");

        boolean result = rateLimiter.allowRequest("10.0.0.1", "/api/auth/login");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("des IPs différentes ont des compteurs indépendants")
    void allowRequest_differentIps_independentBuckets() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.allowRequest("1.1.1.1", "/api/auth/login");
        }

        assertThat(rateLimiter.allowRequest("1.1.1.1", "/api/auth/login")).isFalse();
        assertThat(rateLimiter.allowRequest("2.2.2.2", "/api/auth/login")).isTrue();
    }

    @Test
    @DisplayName("des endpoints différents ont des compteurs indépendants")
    void allowRequest_differentEndpoints_independentBuckets() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.allowRequest("3.3.3.3", "/api/auth/login");
        }
        assertThat(rateLimiter.allowRequest("3.3.3.3", "/api/auth/login")).isFalse();
        assertThat(rateLimiter.allowRequest("3.3.3.3", "/api/auth/verify-otp")).isTrue();
    }

    @Test
    @DisplayName("reset libère le bucket et autorise de nouveau")
    void reset_clearsCount_allowsRequests() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.allowRequest("5.5.5.5", "/api/auth/login");
        }
        assertThat(rateLimiter.allowRequest("5.5.5.5", "/api/auth/login")).isFalse();

        rateLimiter.reset("5.5.5.5", "/api/auth/login");

        assertThat(rateLimiter.allowRequest("5.5.5.5", "/api/auth/login")).isTrue();
    }
}
