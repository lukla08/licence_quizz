package com.example.clickupsimplifier.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

// "WorkspaceList" unika kolizji z java.util.List; mapuje tabelę "list".
@Table("list")
public record WorkspaceList(@Id String id, String name, String spaceId, String folderId) {
}
