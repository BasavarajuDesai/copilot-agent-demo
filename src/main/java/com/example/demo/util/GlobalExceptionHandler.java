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
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", HttpStatus.NOT_FOUND.getReasonPhrase());
        body.put("message", "Resource not found: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
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
                payload.put("body", ex.getMessage());
                // Assign new issues to the 'copilot' user
                payload.put("assignee", "copilot");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                // Use token auth. The token should be provided via env or properties.
                headers.add("Authorization", "token " + githubToken);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

                // Perform the request and capture/check the response
                ResponseEntity<String> githubResponse = restTemplate.postForEntity(url, request, String.class);
                if (githubResponse == null || !githubResponse.getStatusCode().is2xxSuccessful()) {
                    String respBody = (githubResponse == null) ? "<no response>" : githubResponse.getBody();
                    int respStatus = (githubResponse == null) ? -1 : githubResponse.getStatusCodeValue();
                    System.err.println(String.format("Failed to create GitHub issue. Status: %d, Body: %s", respStatus, respBody));
                } else {
                    // Success — optionally inspect response body (JSON) to extract issue URL if needed
                    System.out.println("GitHub issue created successfully. Response status: " + githubResponse.getStatusCodeValue());
                    // System.out.println("Response body: " + githubResponse.getBody());
                }
            }
        } catch (Exception issueEx) {
            // Log to stdout as a best-effort (do not throw). In a real app, use a logger.
            System.err.println("Failed to create GitHub issue: " + issueEx.getMessage());
        }

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
