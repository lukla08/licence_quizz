package com.example.clickupsimplifier.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface WorkspaceListRepository extends CrudRepository<WorkspaceList, String> {

    // sync_enabled intentionally excluded from DO UPDATE — preserves user-set value.
    @Modifying
    @Query("INSERT INTO list (id, name, space_id, folder_id) VALUES (:id, :name, :spaceId, :folderId) " +
           "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, space_id = EXCLUDED.space_id, folder_id = EXCLUDED.folder_id")
    void insertOrUpdate(String id, String name, String spaceId, String folderId);

    // Caller must ensure ids is non-empty. CASCADE removes child tasks.
    @Modifying
    @Query("DELETE FROM list WHERE id NOT IN (:ids)")
    void deleteByIdNotIn(Collection<String> ids);

    @Query("SELECT * FROM list WHERE sync_enabled = true")
    List<WorkspaceList> findAllSyncEnabled();

    @Modifying
    @Query("UPDATE list SET sync_enabled = :enabled WHERE id = :id")
    int updateSyncEnabled(String id, boolean enabled);

    @Query("SELECT * FROM list ORDER BY space_id, folder_id NULLS LAST, name")
    List<WorkspaceList> findAllOrderedForDisplay();

    List<WorkspaceList> findBySpaceId(String spaceId);

    List<WorkspaceList> findByFolderId(String folderId);
}
