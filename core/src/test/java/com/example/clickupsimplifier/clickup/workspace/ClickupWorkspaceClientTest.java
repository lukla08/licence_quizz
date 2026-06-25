package com.example.clickupsimplifier.clickup.workspace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClickupWorkspaceClientTest {

    private static final String BASE = "http://mock-api";
    private static final String TOKEN = "pk_test_token";

    private MockRestServiceServer mockServer;
    private ClickupWorkspaceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new ClickupWorkspaceClient(builder.build());
        client.retryDelaysMs = new long[]{0, 0, 0};
    }

    // --- getTeams ---

    @Test
    void getTeams_correctPathAndAuth() {
        mockServer.expect(requestTo(BASE + "/team"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("""
                        {"teams":[{"id":"t1","name":"My Team"}]}
                        """, MediaType.APPLICATION_JSON));

        List<ClickupTeam> teams = client.getTeams(TOKEN);

        assertThat(teams).hasSize(1);
        assertThat(teams.get(0).id()).isEqualTo("t1");
        assertThat(teams.get(0).name()).isEqualTo("My Team");
        mockServer.verify();
    }

    // --- getSpaces ---

    @Test
    void getSpaces_correctPathAndQueryParam() {
        mockServer.expect(requestTo(allOf(
                        containsString("/team/t1/space"),
                        containsString("archived=false"))))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("""
                        {"spaces":[{"id":"s1","name":"Space One"}]}
                        """, MediaType.APPLICATION_JSON));

        List<ClickupSpace> spaces = client.getSpaces(TOKEN, "t1");

        assertThat(spaces).extracting(ClickupSpace::id).containsExactly("s1");
        mockServer.verify();
    }

    // --- getFolders ---

    @Test
    void getFolders_correctPathAndMapsSpaceId() {
        mockServer.expect(requestTo(allOf(
                        containsString("/space/s1/folder"),
                        containsString("archived=false"))))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("""
                        {"folders":[{"id":"f1","name":"Folder A","space":{"id":"s1"}}]}
                        """, MediaType.APPLICATION_JSON));

        List<ClickupFolder> folders = client.getFolders(TOKEN, "s1");

        assertThat(folders).hasSize(1);
        assertThat(folders.get(0).id()).isEqualTo("f1");
        assertThat(folders.get(0).spaceId()).isEqualTo("s1");
        mockServer.verify();
    }

    // --- getFolderlessLists ---

    @Test
    void getFolderlessLists_hiddenFolderYieldsFolderIdNull() {
        mockServer.expect(requestTo(allOf(
                        containsString("/space/s1/list"),
                        containsString("archived=false"))))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("""
                        {"lists":[{"id":"l1","name":"Folderless","space":{"id":"s1"},"folder":{"id":"none","hidden":true}}]}
                        """, MediaType.APPLICATION_JSON));

        List<ClickupList> lists = client.getFolderlessLists(TOKEN, "s1");

        assertThat(lists).hasSize(1);
        assertThat(lists.get(0).spaceId()).isEqualTo("s1");
        assertThat(lists.get(0).folderId()).isNull();
        mockServer.verify();
    }

    // --- getListsByFolder ---

    @Test
    void getListsByFolder_correctPathAndFolderIdMapped() {
        mockServer.expect(requestTo(allOf(
                        containsString("/folder/f1/list"),
                        containsString("archived=false"))))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("""
                        {"lists":[{"id":"l2","name":"In Folder","space":{"id":"s1"},"folder":{"id":"f1","hidden":false}}]}
                        """, MediaType.APPLICATION_JSON));

        List<ClickupList> lists = client.getListsByFolder(TOKEN, "f1");

        assertThat(lists).hasSize(1);
        assertThat(lists.get(0).folderId()).isEqualTo("f1");
        mockServer.verify();
    }

    // --- getTasks: single page ---

    @Test
    void getTasks_correctParamsAndMapsTask() {
        mockServer.expect(requestTo(allOf(
                        containsString("/list/l1/task"),
                        containsString("include_closed=true"),
                        containsString("subtasks=true"),
                        containsString("page=0"))))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("""
                        {"tasks":[{"id":"t1","name":"My Task","status":{"status":"open"},"description":"desc","milestone":false,"parent":null}],"last_page":true}
                        """, MediaType.APPLICATION_JSON));

        List<ClickupTask> tasks = client.getTasks(TOKEN, "l1", null);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).id()).isEqualTo("t1");
        assertThat(tasks.get(0).statusValue()).isEqualTo("open");
        assertThat(tasks.get(0).milestone()).isFalse();
        assertThat(tasks.get(0).parent()).isNull();
        mockServer.verify();
    }

    // --- getTasks: pagination ---

    @Test
    void getTasks_paginationConcatenatesBothPages() {
        mockServer.expect(requestTo(containsString("page=0")))
                .andRespond(withSuccess("""
                        {"tasks":[{"id":"t1","name":"Task 1","milestone":false}],"last_page":false}
                        """, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("page=1")))
                .andRespond(withSuccess("""
                        {"tasks":[{"id":"t2","name":"Task 2","milestone":false}],"last_page":true}
                        """, MediaType.APPLICATION_JSON));

        List<ClickupTask> tasks = client.getTasks(TOKEN, "l1", null);

        assertThat(tasks).extracting(ClickupTask::id).containsExactly("t1", "t2");
        mockServer.verify();
    }

    // --- getTasks: since param ---

    @Test
    void getTasks_withSince_addsDateUpdatedGt() {
        Instant since = Instant.ofEpochMilli(1_700_000_000_000L);
        mockServer.expect(requestTo(allOf(
                        containsString("/list/l1/task"),
                        containsString("date_updated_gt=1700000000000"))))
                .andRespond(withSuccess("""
                        {"tasks":[],"last_page":true}
                        """, MediaType.APPLICATION_JSON));

        List<ClickupTask> tasks = client.getTasks(TOKEN, "l1", since);

        assertThat(tasks).isEmpty();
        mockServer.verify();
    }

    // --- 429 retry: success on 3rd attempt ---

    @Test
    void withRetry_429TwiceThenSuccess_returnsResult() {
        mockServer.expect(requestTo(BASE + "/team"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        mockServer.expect(requestTo(BASE + "/team"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        mockServer.expect(requestTo(BASE + "/team"))
                .andRespond(withSuccess("""
                        {"teams":[{"id":"t1","name":"Team"}]}
                        """, MediaType.APPLICATION_JSON));

        List<ClickupTeam> teams = client.getTeams(TOKEN);

        assertThat(teams).hasSize(1);
        mockServer.verify();
    }

    // --- 429 retry: exhausted after 3 attempts ---

    @Test
    void withRetry_429ThreeTimes_propagatesException() {
        mockServer.expect(requestTo(BASE + "/team"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        mockServer.expect(requestTo(BASE + "/team"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        mockServer.expect(requestTo(BASE + "/team"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.getTeams(TOKEN))
                .isInstanceOf(RestClientResponseException.class)
                .satisfies(e -> assertThat(((RestClientResponseException) e).getStatusCode().value()).isEqualTo(429));
        mockServer.verify();
    }
}
