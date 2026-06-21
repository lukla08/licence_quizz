package com.example.clickupsimplifier.persistence;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface SpaceRepository extends CrudRepository<Space, String> {

    @Modifying
    @Query("INSERT INTO space (id, name) VALUES (:id, :name) " +
           "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name")
    void upsert(String id, String name);
}
