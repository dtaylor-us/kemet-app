package com.kemet.core.practice;

import com.kemet.core.domain.AppUser;
import com.kemet.core.domain.PracticeState;
import com.kemet.core.repository.PracticeStateRepository;
import com.kemet.core.user.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Marks today's practice complete for whichever faculty the user currently has active.
 * Feeds PracticeState.completedDays / lastPracticedAt, which the companion's system
 * prompt uses for personalization (REQ-050, REQ-071-075).
 *
 * Date comparison uses UTC (via ZoneOffset.UTC) because lastPracticedAt is stored
 * as an Instant (inherently UTC), and no other timezone convention exists in this
 * codebase. This is an open question if multi-timezone user support is added later.
 */
@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    private final UserService userService;
    private final PracticeStateRepository practiceStateRepository;

    public PracticeController(UserService userService, PracticeStateRepository practiceStateRepository) {
        this.userService = userService;
        this.practiceStateRepository = practiceStateRepository;
    }

    @PostMapping("/complete")
    public PracticeState complete(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = userService.getOrCreate(jwt);
        String facultyId = user.getActiveFacultyId();

        PracticeState state = practiceStateRepository.findByUserIdAndFacultyId(user.getId(), facultyId)
                .orElseGet(() -> {
                    PracticeState fresh = new PracticeState();
                    fresh.setUserId(user.getId());
                    fresh.setFacultyId(facultyId);
                    return fresh;
                });

        if (state.getLastPracticedAt() != null) {
            LocalDate lastDate = state.getLastPracticedAt().atZone(ZoneOffset.UTC).toLocalDate();
            if (lastDate.equals(LocalDate.now(ZoneOffset.UTC))) {
                return state; // already completed today — return unchanged (idempotent)
            }
        }

        state.setCompletedDays(state.getCompletedDays() + 1);
        state.setLastPracticedAt(Instant.now());
        return practiceStateRepository.save(state);
    }
}
