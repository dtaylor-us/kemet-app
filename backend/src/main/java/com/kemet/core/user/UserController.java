package com.kemet.core.user;

import com.kemet.core.domain.AppUser;
import com.kemet.core.repository.FacultyContentRepository;
import com.kemet.core.user.dto.SetActiveFacultyRequest;
import com.kemet.core.user.dto.UserProfile;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final FacultyContentRepository facultyContentRepository;

    public UserController(UserService userService, FacultyContentRepository facultyContentRepository) {
        this.userService = userService;
        this.facultyContentRepository = facultyContentRepository;
    }

    @GetMapping("/me")
    public UserProfile me(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = userService.getOrCreate(jwt);
        return new UserProfile(user.getId(), user.getDisplayName(), user.getActiveFacultyId());
    }

    @PatchMapping("/active-faculty")
    public UserProfile setActiveFaculty(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody SetActiveFacultyRequest request) {
        AppUser user = userService.getOrCreate(jwt);

        if (!facultyContentRepository.existsById(request.facultyId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown faculty id: " + request.facultyId());
        }

        user.setActiveFacultyId(request.facultyId());
        userService.save(user);
        return new UserProfile(user.getId(), user.getDisplayName(), user.getActiveFacultyId());
    }
}
