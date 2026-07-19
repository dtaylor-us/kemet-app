package com.kemet.core.companion;

import com.kemet.core.companion.dto.ChatRequest;
import com.kemet.core.companion.dto.ChatResponse;
import com.kemet.core.domain.AppUser;
import com.kemet.core.user.UserService;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/companion")
public class CompanionController {

    private final CompanionService companionService;
    private final UserService userService;

    public CompanionController(CompanionService companionService, UserService userService) {
        this.companionService = companionService;
        this.userService = userService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChatRequest request)
            throws IOException, InterruptedException {

        AppUser user = userService.getOrCreate(jwt);
        String reply = companionService.reply(user, request.message());
        return ChatResponse.of(reply);
    }
}
