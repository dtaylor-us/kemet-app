package com.kemet.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void ioExceptionReturnsBadGatewayWithCompanionUnavailableError() throws Exception {
        ResponseEntity<Map<String, String>> response =
                handler.handleCompanionUnavailable(new IOException("connection reset"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("error", "companion_unavailable");
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    void interruptedExceptionReturnsBadGatewayAndRestoresInterruptFlag() throws Exception {
        ResponseEntity<Map<String, String>> response =
                handler.handleCompanionUnavailable(new InterruptedException("timed out"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("error", "companion_unavailable");
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted(); // clear the flag after asserting
    }

    @Test
    void illegalStateExceptionReturnsInternalErrorWithMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalState(new IllegalStateException("Faculty content 'amen' not seeded"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "internal_error");
        assertThat(response.getBody()).containsEntry("message", "Faculty content 'amen' not seeded");
    }

    @Test
    void unexpectedExceptionReturnsGenericInternalError() throws Exception {
        ResponseEntity<Map<String, String>> response =
                handler.handleUnexpected(new RuntimeException("something unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "unexpected_error");
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    void responseStatusExceptionIsReThrownByGenericHandler() {
        ResponseStatusException rse = new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad input");
        assertThatThrownBy(() -> handler.handleUnexpected(rse))
                .isSameAs(rse);
    }
}
