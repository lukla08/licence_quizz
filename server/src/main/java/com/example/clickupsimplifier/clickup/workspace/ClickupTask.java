package com.example.clickupsimplifier.clickup.workspace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClickupTask(
        String id,
        String name,
        StatusRef status,
        String description,
        boolean milestone,
        String parent,
        List<ClickupTask> subtasks) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusRef(String status) {}

    public String statusValue() { return status != null ? status.status() : null; }
}
