package com.kemet.core.practice;

import com.kemet.core.domain.AppUser;
import com.kemet.core.domain.PracticeState;
import com.kemet.core.repository.PracticeStateRepository;
import com.kemet.core.user.UserService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PracticeControllerTest {
    private final UserService users = mock(UserService.class);
    private final PracticeStateRepository states = mock(PracticeStateRepository.class);
    private final PracticeController controller = new PracticeController(users, states);

    @Test
    void incrementsExistingPracticeState() {
        AppUser user = user("maat");
        PracticeState state = new PracticeState();
        state.setCompletedDays(2);
        when(users.getOrCreate(any())).thenReturn(user);
        when(states.findByUserIdAndFacultyId(user.getId(), "maat")).thenReturn(Optional.of(state));
        when(states.save(state)).thenReturn(state);

        PracticeState result = controller.complete(null);
        assertThat(result.getCompletedDays()).isEqualTo(3);
        assertThat(result.getLastPracticedAt()).isNotNull();
    }

    @Test
    void createsStateForFirstPractice() {
        AppUser user = user("amen");
        when(users.getOrCreate(any())).thenReturn(user);
        when(states.findByUserIdAndFacultyId(user.getId(), "amen")).thenReturn(Optional.empty());
        when(states.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PracticeState result = controller.complete(null);
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getFacultyId()).isEqualTo("amen");
        assertThat(result.getCompletedDays()).isOne();
    }

    @Test
    void secondCompletionOnSameDayDoesNotIncrement() {
        AppUser user = user("maat");
        PracticeState state = new PracticeState();
        state.setCompletedDays(5);
        // lastPracticedAt is today (UTC)
        state.setLastPracticedAt(LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC).plus(java.time.Duration.ofHours(1)));
        when(users.getOrCreate(any())).thenReturn(user);
        when(states.findByUserIdAndFacultyId(user.getId(), "maat")).thenReturn(Optional.of(state));

        PracticeState result = controller.complete(null);
        assertThat(result.getCompletedDays()).isEqualTo(5);
        verify(states, never()).save(any());
    }

    @Test
    void completionOnNewDayAfterPriorDayIncrements() {
        AppUser user = user("maat");
        PracticeState state = new PracticeState();
        state.setCompletedDays(3);
        // lastPracticedAt is yesterday (UTC)
        state.setLastPracticedAt(Instant.now().atZone(ZoneOffset.UTC).toLocalDate().minusDays(1)
                .atStartOfDay().toInstant(ZoneOffset.UTC));
        when(users.getOrCreate(any())).thenReturn(user);
        when(states.findByUserIdAndFacultyId(user.getId(), "maat")).thenReturn(Optional.of(state));
        when(states.save(state)).thenReturn(state);

        PracticeState result = controller.complete(null);
        assertThat(result.getCompletedDays()).isEqualTo(4);
        assertThat(result.getLastPracticedAt()).isNotNull();
    }

    private AppUser user(String faculty) {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setActiveFacultyId(faculty);
        return user;
    }
}
