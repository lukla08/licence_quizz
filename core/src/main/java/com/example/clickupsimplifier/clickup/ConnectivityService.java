package com.example.clickupsimplifier.clickup;

import com.example.clickupsimplifier.settings.SettingsStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Service
public class ConnectivityService {

    private final SettingsStore settingsStore;
    private final ClickupClient clickupClient;

    public ConnectivityService(SettingsStore settingsStore, ClickupClient clickupClient) {
        this.settingsStore = settingsStore;
        this.clickupClient = clickupClient;
    }

    public ConnectivityResult checkConnectivity() {
        Optional<String> token = settingsStore.getToken();
        if (token.isEmpty()) {
            return ConnectivityResult.notConfigured();
        }
        try {
            ClickupUser user = clickupClient.getCurrentUser(token.get());
            return ConnectivityResult.ok(user);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return ConnectivityResult.tokenRejected();
            }
            return ConnectivityResult.unreachable();
        } catch (RestClientException e) {
            return ConnectivityResult.unreachable();
        }
    }
}
