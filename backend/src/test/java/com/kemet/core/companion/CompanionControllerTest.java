package com.kemet.core.companion;

import com.kemet.core.companion.dto.ChatRequest;
import com.kemet.core.domain.AppUser;
import com.kemet.core.user.UserService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompanionControllerTest {
    @Test
    void labelsCompanionReplyAsAiGenerated() throws Exception {
        CompanionService service = mock(CompanionService.class);
        UserService users = mock(UserService.class);
        AppUser user = new AppUser();
        when(users.getOrCreate(any())).thenReturn(user);
        when(service.reply(user, "hello")).thenReturn("reply");

        var response = new CompanionController(service, users).chat(null, new ChatRequest("hello"));
        assertThat(response.reply()).isEqualTo("reply");
        assertThat(response.aiGenerated()).isTrue();
    }
}
