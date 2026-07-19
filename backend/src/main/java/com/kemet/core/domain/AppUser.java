package com.kemet.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A single student. For this prototype there is exactly one role (student) and one
 * test account — REQ-004's public/course/chapter entitlement tiers and the
 * teacher/admin roles from REQ-006/007/025/026 are intentionally not modeled yet.
 * See the architecture spec's "out of scope" notes before extending this.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue
    private UUID id;

    /** The "sub" claim from the Auth0-issued JWT — the durable link to the identity provider. */
    @Column(nullable = false, unique = true)
    private String auth0Subject;

    @Column(nullable = false)
    private String displayName;

    /** Which of the 11 seeded faculties the user is currently practicing. Defaults to
     * "amen" — the workbook's own recommended starting point — until they switch. */
    @Column(nullable = false)
    private String activeFacultyId = "amen";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
