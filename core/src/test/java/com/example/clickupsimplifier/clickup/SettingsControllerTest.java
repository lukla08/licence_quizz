package com.example.clickupsimplifier.clickup;

import com.example.clickupsimplifier.settings.SettingsStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettingsController.class)
class SettingsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SettingsStore settingsStore;

    @MockitoBean
    ConnectivityService connectivityService;

    @Test
    void putValidTokenReturns204() throws Exception {
        mockMvc.perform(put("/api/settings/clickup-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "pk_valid"}
                                """))
                .andExpect(status().isNoContent());

        verify(settingsStore).saveToken("pk_valid");
    }

    @Test
    void putEmptyTokenReturns400() throws Exception {
        doThrow(new IllegalArgumentException("blank token"))
                .when(settingsStore).saveToken("");

        mockMvc.perform(put("/api/settings/clickup-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getConnectivityReturnsOkStatusWithUser() throws Exception {
        ClickupUser user = new ClickupUser("123", "john");
        when(connectivityService.checkConnectivity())
                .thenReturn(ConnectivityResult.ok(user));

        mockMvc.perform(get("/api/clickup/connectivity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.user.id").value("123"))
                .andExpect(jsonPath("$.user.username").value("john"));
    }

    @Test
    void getConnectivityReturnsTokenRejectedStatus() throws Exception {
        when(connectivityService.checkConnectivity())
                .thenReturn(ConnectivityResult.tokenRejected());

        mockMvc.perform(get("/api/clickup/connectivity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TOKEN_REJECTED"))
                .andExpect(jsonPath("$.message").value("Token odrzucony przez ClickUp (401)"));
    }
}
