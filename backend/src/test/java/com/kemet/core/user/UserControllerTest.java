package com.kemet.core.user;

import com.kemet.core.domain.AppUser;
import com.kemet.core.repository.FacultyContentRepository;
import com.kemet.core.user.dto.SetActiveFacultyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {
    private final UserService userService = mock(UserService.class);
    private final FacultyContentRepository faculties = mock(FacultyContentRepository.class);
    private final UserController controller = new UserController(userService, faculties);
    private final AppUser user = new AppUser();

    @BeforeEach
    void setUp() {
        user.setId(UUID.randomUUID());
        user.setDisplayName("Amina");
        when(userService.getOrCreate(any())).thenReturn(user);
    }

    @Test
    void returnsCurrentProfile() {
        var profile = controller.me(UserServiceTest.jwt("sub", "Amina"));
        assertThat(profile.id()).isEqualTo(user.getId());
        assertThat(profile.activeFacultyId()).isEqualTo("amen");
    }

    @Test
    void changesActiveFaculty() {
        when(faculties.existsById("maat")).thenReturn(true);
        var profile = controller.setActiveFaculty(UserServiceTest.jwt("sub", "Amina"),
                new SetActiveFacultyRequest("maat"));
        assertThat(profile.activeFacultyId()).isEqualTo("maat");
        verify(userService).save(user);
    }

    @Test
    void rejectsUnknownFaculty() {
        when(faculties.existsById("unknown")).thenReturn(false);
        assertThatThrownBy(() -> controller.setActiveFaculty(UserServiceTest.jwt("sub", "Amina"),
                new SetActiveFacultyRequest("unknown")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
        verify(userService, never()).save(any());
    }
}
