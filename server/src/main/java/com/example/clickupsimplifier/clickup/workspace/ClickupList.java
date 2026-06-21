package com.example.clickupsimplifier.clickup.workspace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClickupList(String id, String name, ClickupRef space, FolderRef folder) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FolderRef(String id, boolean hidden) {}

    public String spaceId() { return space.id(); }

    public String folderId() { return (folder != null && !folder.hidden()) ? folder.id() : null; }
}
