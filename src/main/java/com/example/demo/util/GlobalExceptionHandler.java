package com.example.demo.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Read GitHub configuration from application.properties or environment variables:
    // - github.owner (or GITHUB_OWNER env)
    // - github.repo  (or GITHUB_REPO env)
    // - GITHUB_TOKEN (must be set in env or properties)
    @Value("${github.owner:}")
    private String githubOwner;

    @Value("${github.repo:}")
    private String githubRepo;

    @Value("${GITHUB_TOKEN:}")
    private String githubToken;

    private final RestTemplate restTemplate = new RestTemplate();

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> body = new LinkedHashMap<>();

        HttpStatusCode statusCode = ex.getStatusCode();
        int statusValue = statusCode.value();
        String reason = (HttpStatus.resolve(statusValue) != null)
                ? HttpStatus.resolve(statusValue).getReasonPhrase()
                : statusCode.toString();

        body.put("timestamp", Instant.now().toString());
        body.put("status", statusValue);
        body.put("error", reason);
        body.put("message", ex.getReason());
        return ResponseEntity.status(statusCode).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        // Build the error response body as before
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        body.put("message", ex.getMessage());

        // Try to create a GitHub issue for visibility if configuration is provided.
        // Failures here are caught and ignored so we don't mask the original error handling.
        try {
            if (githubOwner != null && !githubOwner.isBlank()
                    && githubRepo != null && !githubRepo.isBlank()
                    && githubToken != null && !githubToken.isBlank()) {

                String url = String.format("https://api.github.com/repos/%s/%s/issues", githubOwner, githubRepo);

                Map<String, Object> payload = new LinkedHashMap<>();
                String title = String.format("Automated: Exception - %s: %s",
                        ex.getClass().getSimpleName(),
                        ex.getMessage() == null ? "<no message>" : ex.getMessage());

                String stack = Arrays.stream(ex.getStackTrace())
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n"));

                StringBuilder bodyText = new StringBuilder();
                bodyText.append("Timestamp: ").append(Instant.now().toString()).append("\n\n");
                bodyText.append("Message: ").append(ex.getMessage()).append("\n\n");
                bodyText.append("Stacktrace:\n```java\n").append(stack).append("\n```\n");

                payload.put("title", title);
                payload.put("body", bodyText.toString());
                // Assign new issues to the 'copilot' user
                payload.put("assignees", Arrays.asList("copilot"));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                // Use token auth. The token should be provided via env or properties.
                headers.add("Authorization", "token " + githubToken);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

                // Perform the request (we don't examine the response further)
                restTemplate.postForEntity(url, request, String.class);
            }
        } catch (Exception issueEx) {
            // Log to stdout as a best-effort (do not throw). In a real app, use a logger.
            System.err.println("Failed to create GitHub issue: " + issueEx.getMessage());
        }

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
