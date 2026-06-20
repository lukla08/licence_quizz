package com.example.clickupsimplifier.clickup;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ClickupClient {

    private final RestClient restClient;

    public ClickupClient(RestClient clickupRestClient) {
        this.restClient = clickupRestClient;
    }

    public ClickupUser getCurrentUser(String token) {
        UserResponse response = restClient.get()
                .uri("/user")
                .header("Authorization", token)
                .retrieve()
                .body(UserResponse.class);
        if (response == null || response.user() == null) {
            throw new RestClientException("Nieoczekiwana odpowiedź ClickUp: brak pola user");
        }
        return new ClickupUser(String.valueOf(response.user().id()), response.user().username());
    }

    record UserResponse(UserBody user) {
        record UserBody(long id, String username) {}
    }
}
