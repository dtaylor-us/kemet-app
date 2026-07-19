package com.kemet.core.user;

import com.kemet.core.domain.AppUser;
import com.kemet.core.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private final AppUserRepository repository = mock(AppUserRepository.class);
    private final UserService service = new UserService(repository);

    @Test
    void returnsExistingUserWithoutSaving() {
        AppUser existing = new AppUser();
        when(repository.findByAuth0Subject("auth0|1")).thenReturn(Optional.of(existing));

        assertThat(service.getOrCreate(jwt("auth0|1", "Amina"))).isSameAs(existing);
        verify(repository, never()).save(any());
    }

    @Test
    void createsUserFromClaims() {
        when(repository.findByAuth0Subject("auth0|2")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser created = service.getOrCreate(jwt("auth0|2", "Derek"));

        assertThat(created.getAuth0Subject()).isEqualTo("auth0|2");
        assertThat(created.getDisplayName()).isEqualTo("Derek");
        assertThat(created.getActiveFacultyId()).isEqualTo("amen");
    }

    @Test
    void usesStudentWhenNameClaimIsAbsent() {
        when(repository.findByAuth0Subject("auth0|3")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.getOrCreate(jwt("auth0|3", null)).getDisplayName()).isEqualTo("Student");
    }

    static Jwt jwt(String subject, String name) {
        Map<String, Object> claims = name == null ? Map.of("sub", subject) : Map.of("sub", subject, "name", name);
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"), claims);
    }
}
