package com.kemet.core.repository;

import com.kemet.core.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);
}
