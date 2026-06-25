package com.example.clickupsimplifier.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface FolderRepository extends CrudRepository<Folder, String> {

    @Modifying
    @Query("INSERT INTO folder (id, space_id, name) VALUES (:id, :spaceId, :name) " +
           "ON CONFLICT (id) DO UPDATE SET space_id = EXCLUDED.space_id, name = EXCLUDED.name")
    void insertOrUpdate(String id, String spaceId, String name);

    // Caller must ensure ids is non-empty. CASCADE removes child lists/tasks.
    @Modifying
    @Query("DELETE FROM folder WHERE id NOT IN (:ids)")
    void deleteByIdNotIn(Collection<String> ids);

    List<Folder> findBySpaceId(String spaceId);
}
