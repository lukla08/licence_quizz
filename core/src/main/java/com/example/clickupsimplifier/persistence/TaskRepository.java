package com.example.clickupsimplifier.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface TaskRepository extends CrudRepository<Task, String> {

    @Modifying
    @Query("INSERT INTO task (id, list_id, name, status, description, is_milestone, milestone_id, parent_id) " +
           "VALUES (:id, :listId, :name, :status, :description, :isMilestone, :milestoneId, :parentId) " +
           "ON CONFLICT (id) DO UPDATE SET " +
           "list_id = EXCLUDED.list_id, name = EXCLUDED.name, status = EXCLUDED.status, " +
           "description = EXCLUDED.description, is_milestone = EXCLUDED.is_milestone, " +
           "milestone_id = EXCLUDED.milestone_id, parent_id = EXCLUDED.parent_id")
    void insertOrUpdate(String id, String listId, String name, String status, String description,
                       boolean isMilestone, String milestoneId, String parentId);

    // Caller must ensure ids is non-empty; use deleteByListId for the all-tasks case.
    @Modifying
    @Query("DELETE FROM task WHERE list_id = :listId AND id NOT IN (:ids)")
    void deleteStaleByListId(String listId, Collection<String> ids);

    @Modifying
    @Query("DELETE FROM task WHERE list_id = :listId")
    void deleteByListId(String listId);

    List<Task> findByListId(String listId);

    List<Task> findByMilestoneId(String milestoneId);

    List<Task> findByListIdAndIsMilestoneTrue(String listId);
}
