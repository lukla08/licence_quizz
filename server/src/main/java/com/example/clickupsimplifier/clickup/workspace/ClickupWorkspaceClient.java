package com.example.clickupsimplifier.clickup.workspace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Component
public class ClickupWorkspaceClient {

    private final RestClient restClient;

    // Package-private to allow test override (zero-delay fast tests)
    long[] retryDelaysMs = {1_000, 2_000, 4_000};

    public ClickupWorkspaceClient(RestClient clickupRestClient) {
        this.restClient = clickupRestClient;
    }

    public List<ClickupTeam> getTeams(String token) {
        TeamsResponse body = withRetry(() -> restClient.get()
                .uri("/team")
                .header("Authorization", token)
                .retrieve()
                .body(TeamsResponse.class));
        if (body == null) throw new RestClientException("Unexpected ClickUp response: missing teams body");
        return body.teams();
    }

    public List<ClickupSpace> getSpaces(String token, String teamId) {
        SpacesResponse body = withRetry(() -> restClient.get()
                .uri("/team/{teamId}/space?archived=false", teamId)
                .header("Authorization", token)
                .retrieve()
                .body(SpacesResponse.class));
        if (body == null) throw new RestClientException("Unexpected ClickUp response: missing spaces body");
        return body.spaces();
    }

    public List<ClickupFolder> getFolders(String token, String spaceId) {
        FoldersResponse body = withRetry(() -> restClient.get()
                .uri("/space/{spaceId}/folder?archived=false", spaceId)
                .header("Authorization", token)
                .retrieve()
                .body(FoldersResponse.class));
        if (body == null) throw new RestClientException("Unexpected ClickUp response: missing folders body");
        return body.folders();
    }

    public List<ClickupList> getFolderlessLists(String token, String spaceId) {
        ListsResponse body = withRetry(() -> restClient.get()
                .uri("/space/{spaceId}/list?archived=false", spaceId)
                .header("Authorization", token)
                .retrieve()
                .body(ListsResponse.class));
        if (body == null) throw new RestClientException("Unexpected ClickUp response: missing lists body");
        return body.lists();
    }

    public List<ClickupList> getListsByFolder(String token, String folderId) {
        ListsResponse body = withRetry(() -> restClient.get()
                .uri("/folder/{folderId}/list?archived=false", folderId)
                .header("Authorization", token)
                .retrieve()
                .body(ListsResponse.class));
        if (body == null) throw new RestClientException("Unexpected ClickUp response: missing lists body");
        return body.lists();
    }

    public List<ClickupTask> getTasks(String token, String listId, Instant since) {
        List<ClickupTask> all = new ArrayList<>();
        int page = 0;
        while (true) {
            final int currentPage = page;
            TasksPage tp = withRetry(() -> restClient.get()
                    .uri(b -> {
                        var ub = b.path("/list/{listId}/task")
                                .queryParam("include_closed", "true")
                                .queryParam("subtasks", "true")
                                .queryParam("page", currentPage);
                        if (since != null) {
                            ub = ub.queryParam("date_updated_gt", since.toEpochMilli());
                        }
                        return ub.build(listId);
                    })
                    .header("Authorization", token)
                    .retrieve()
                    .body(TasksPage.class));
            if (tp == null) throw new RestClientException("Unexpected ClickUp response: missing tasks body");
            all.addAll(tp.tasks());
            if (tp.lastPage()) break;
            page++;
        }
        return all;
    }

    private <T> T withRetry(Supplier<T> call) {
        if (retryDelaysMs.length == 0) return call.get();
        RestClientResponseException lastEx = null;
        for (int attempt = 0; attempt < retryDelaysMs.length; attempt++) {
            try {
                return call.get();
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().value() != 429) throw e;
                lastEx = e;
                if (attempt < retryDelaysMs.length - 1) {
                    try {
                        Thread.sleep(retryDelaysMs[attempt]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        throw lastEx;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TeamsResponse(List<ClickupTeam> teams) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpacesResponse(List<ClickupSpace> spaces) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FoldersResponse(List<ClickupFolder> folders) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ListsResponse(List<ClickupList> lists) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TasksPage(List<ClickupTask> tasks, @JsonProperty("last_page") boolean lastPage) {}
}
