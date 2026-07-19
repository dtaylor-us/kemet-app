package com.kemet.core.faculty.dto;

/** Lightweight listing entry for the faculty picker — the full content (hekau,
 * meditation instructions, etc.) is only fetched via GET /api/faculty/{id} once the
 * user actually selects one, to keep the list payload small. */
public record FacultySummary(String id, String displayName, String role, int practiceOrder) {
}
