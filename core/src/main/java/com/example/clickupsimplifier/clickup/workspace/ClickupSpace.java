package com.example.clickupsimplifier.clickup.workspace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClickupSpace(String id, String name) {}
