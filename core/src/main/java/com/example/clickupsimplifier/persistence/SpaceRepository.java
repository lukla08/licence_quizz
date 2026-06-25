package com.example.clickupsimplifier.persistence;

import java.util.Collection;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface SpaceRepository extends CrudRepository<Space, String> {

    @Modifying
    @Query("INSERT INTO space (id, name) VALUES (:id, :name) " +
           "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name")
    void insertOrUpdate(String id, String name);

    // Caller must ensure ids is non-empty. CASCADE removes child folders/lists/tasks.
    @Modifying
    @Query("DELETE FROM space WHERE id NOT IN (:ids)")
    void deleteByIdNotIn(Collection<String> ids);
}
