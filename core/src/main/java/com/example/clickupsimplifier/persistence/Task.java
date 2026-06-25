package com.example.clickupsimplifier.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

@Table("task")
public record Task(
        @Id String id,
        String listId,
        String name,
        @Nullable String status,
        @Nullable String description,
        boolean isMilestone,
        @Nullable String milestoneId,
        @Nullable String parentId
) {
}
