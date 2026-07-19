package com.kemet.core.user;

import com.kemet.core.domain.AppUser;
import com.kemet.core.repository.AppUserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Find-or-create for the AppUser behind an Auth0 JWT. Pulled out of
 * CompanionController once PracticeController and the new active-faculty endpoint
 * needed the same lookup — three copies of the same inline logic was the DRY violation
 * that triggered this extraction, not a speculative "might need it later."
 */
@Service
public class UserService {

    private final AppUserRepository appUserRepository;

    public UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser getOrCreate(Jwt jwt) {
        return appUserRepository.findByAuth0Subject(jwt.getSubject())
                .orElseGet(() -> {
                    AppUser created = new AppUser();
                    created.setAuth0Subject(jwt.getSubject());
                    created.setDisplayName(jwt.getClaimAsString("name") != null
                            ? jwt.getClaimAsString("name") : "Student");
                    return appUserRepository.save(created);
                });
    }
}
