package com.example.clickupsimplifier.sync.dto;

import org.springframework.lang.Nullable;

public record ListResponse(
        String id,
        String name,
        boolean syncEnabled,
        @Nullable String folderId,
        String spaceId
) {}
