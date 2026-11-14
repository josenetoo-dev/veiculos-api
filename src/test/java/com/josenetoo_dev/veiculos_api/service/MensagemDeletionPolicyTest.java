package com.josenetoo_dev.veiculos_api.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MensagemDeletionPolicyTest {
    private final MensagemDeletionPolicy policy = new MensagemDeletionPolicy();
    private final LocalDateTime sentAt = LocalDateTime.of(2026, 11, 12, 12, 0);

    @Test
    void allowsAuthorToDeleteAtExactFiveMinuteBoundary() {
        assertTrue(policy.canDelete(7L, 7L, sentAt, sentAt.plusMinutes(5)));
    }

    @Test
    void rejectsDeletionAfterFiveMinutes() {
        assertFalse(policy.canDelete(7L, 7L, sentAt, sentAt.plusMinutes(5).plusNanos(1)));
    }

    @Test
    void rejectsDeletionByAnotherSender() {
        assertFalse(policy.canDelete(8L, 7L, sentAt, sentAt.plusMinutes(1)));
    }
}
