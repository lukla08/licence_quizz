package com.example.clickupsimplifier.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("task")
public record Task(
        @Id String id,
        String listId,
        String name,
        String status,
        String description,
        boolean isMilestone,
        String milestoneId
) {
}
