package com.example.clickupsimplifier.clickup;

import com.example.clickupsimplifier.config.ClickupProperties;
import com.example.clickupsimplifier.settings.SettingsStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ConnectivityServiceTest {

    private MockRestServiceServer mockServer;
    private ConnectivityService service;
    private SettingsStore store;

    @BeforeEach
    void setUp(@TempDir Path tmp) {
        ClickupProperties props = new ClickupProperties(
                tmp.resolve("settings.json").toString(),
                new ClickupProperties.Api("http://mock-clickup")
        );
        store = new SettingsStore(props, JsonMapper.builder().build());

        RestClient.Builder builder = RestClient.builder().baseUrl("http://mock-clickup");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        ClickupClient clickupClient = new ClickupClient(restClient);
        service = new ConnectivityService(store, clickupClient);
    }

    @Test
    void returnsOkWithUserWhenTokenValid() {
        store.saveToken("pk_valid_token");
        mockServer.expect(requestTo("http://mock-clickup/user"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "pk_valid_token"))
                .andRespond(withSuccess("""
                        {"user": {"id": 123, "username": "john"}}
                        """, MediaType.APPLICATION_JSON));

        ConnectivityResult result = service.checkConnectivity();

        assertThat(result.status()).isEqualTo(ConnectivityResult.Status.OK);
        assertThat(result.user().id()).isEqualTo("123");
        assertThat(result.user().username()).isEqualTo("john");
        mockServer.verify();
    }

    @Test
    void returnsTokenRejectedOn401() {
        store.saveToken("pk_bad_token");
        mockServer.expect(requestTo("http://mock-clickup/user"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        ConnectivityResult result = service.checkConnectivity();

        assertThat(result.status()).isEqualTo(ConnectivityResult.Status.TOKEN_REJECTED);
        mockServer.verify();
    }

    @Test
    void returnsNotConfiguredWhenNoToken() {
        ConnectivityResult result = service.checkConnectivity();

        assertThat(result.status()).isEqualTo(ConnectivityResult.Status.NOT_CONFIGURED);
        mockServer.verify();
    }

    @Test
    void returnsUnreachableOnNetworkError() {
        store.saveToken("pk_any_token");
        mockServer.expect(requestTo("http://mock-clickup/user"))
                .andRespond(request -> { throw new ResourceAccessException("simulated network error"); });

        ConnectivityResult result = service.checkConnectivity();

        assertThat(result.status()).isEqualTo(ConnectivityResult.Status.UNREACHABLE);
        mockServer.verify();
    }
}
