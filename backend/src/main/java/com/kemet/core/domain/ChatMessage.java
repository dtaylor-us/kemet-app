package com.kemet.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One turn of companion conversation history (REQ-071: "the AI should remember previous
 * conversations"). Kept as flat role/content rows — simplest thing that lets
 * CompanionService reconstruct the last N turns for the OpenAI API's messages array.
 */
@Entity
@Table(name = "chat_message")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String role; // "user" or "assistant"

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
