package com.example.clickupsimplifier.clickup.workspace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClickupFolder(String id, String name, ClickupRef space) {

    public String spaceId() { return space.id(); }
}
