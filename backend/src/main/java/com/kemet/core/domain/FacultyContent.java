package com.kemet.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Approved teaching content for one Neteru/faculty (REQ-003). Stored as a raw JSON blob
 * rather than fully normalized columns — deliberate for a v0 prototype with a single
 * seeded faculty (Amen) and no teacher-authoring UI yet (REQ-019/020/021 are out of
 * scope for this slice; content is seeded from amen-faculty-seed.json, not authored
 * in-app). Normalize this once a second faculty or teacher editing is in scope.
 */
@Entity
@Table(name = "faculty_content")
@Getter
@Setter
@NoArgsConstructor
public class FacultyContent {

    @Id
    private String id; // e.g. "amen" — matches the seed file's faculty.id

    @Column(nullable = false)
    private String displayName;

    /** Full structured content (teachingNotes, hekau, meditationInstructions, journalPrompts, etc.) as JSON text. */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentJson;
}
