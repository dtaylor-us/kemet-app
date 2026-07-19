package com.kemet.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * What the companion needs to "remember" about a user's practice (REQ-071-075, 078):
 * current faculty focus, how many days completed, and when they last practiced. This is
 * intentionally minimal for v0 — no assigned-by-teacher program, no completed-lessons
 * list, no bookmarks (REQ-078's fuller list) until the content/roles side of the product
 * is in scope.
 */
@Entity
@Table(name = "practice_state")
@Getter
@Setter
@NoArgsConstructor
public class PracticeState {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String facultyId; // one of the 11 seeded faculty ids — a user can have a PracticeState row per faculty they've practiced

    @Column(nullable = false)
    private int completedDays = 0;

    private Instant lastPracticedAt;

    @Column(nullable = false)
    private boolean journalAnalysisOptIn = false; // REQ-052 — off by default
}
