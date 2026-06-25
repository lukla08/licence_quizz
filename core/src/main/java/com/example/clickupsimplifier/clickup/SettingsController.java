package com.example.clickupsimplifier.clickup;

import com.example.clickupsimplifier.clickup.dto.ConnectivityResponse;
import com.example.clickupsimplifier.clickup.dto.SetTokenRequest;
import com.example.clickupsimplifier.settings.SettingsStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class SettingsController {

    private final SettingsStore settingsStore;
    private final ConnectivityService connectivityService;

    SettingsController(SettingsStore settingsStore, ConnectivityService connectivityService) {
        this.settingsStore = settingsStore;
        this.connectivityService = connectivityService;
    }

    @PutMapping("/settings/clickup-token")
    ResponseEntity<Void> saveToken(@RequestBody SetTokenRequest request) {
        try {
            settingsStore.saveToken(request.token());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/clickup/connectivity")
    ConnectivityResponse checkConnectivity() {
        ConnectivityResult result = connectivityService.checkConnectivity();
        return new ConnectivityResponse(
                result.status().name(),
                result.user(),
                result.message()
        );
    }
}
