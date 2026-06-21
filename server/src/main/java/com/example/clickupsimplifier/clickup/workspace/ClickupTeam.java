package com.example.clickupsimplifier.clickup.workspace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClickupTeam(String id, String name) {}
