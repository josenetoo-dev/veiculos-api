package com.josenetoo_dev.veiculos_api.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MensagemDeletionPolicy {
    public boolean canDelete(Long senderId, Long currentUserId, LocalDateTime createdAt, LocalDateTime now) {
        return senderId != null && senderId.equals(currentUserId)
                && createdAt != null && now != null
                && !now.isAfter(createdAt.plusMinutes(5));
    }
}
