package com.example.clickupsimplifier.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("folder")
public record Folder(@Id String id, String spaceId, String name) {
}
