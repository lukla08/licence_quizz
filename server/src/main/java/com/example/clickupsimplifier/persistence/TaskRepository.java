package com.example.clickupsimplifier.persistence;

import java.util.List;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface TaskRepository extends CrudRepository<Task, String> {

    @Modifying
    @Query("INSERT INTO task (id, list_id, name, status, description, is_milestone, milestone_id) " +
           "VALUES (:id, :listId, :name, :status, :description, :isMilestone, :milestoneId) " +
           "ON CONFLICT (id) DO UPDATE SET " +
           "list_id = EXCLUDED.list_id, name = EXCLUDED.name, status = EXCLUDED.status, " +
           "description = EXCLUDED.description, is_milestone = EXCLUDED.is_milestone, " +
           "milestone_id = EXCLUDED.milestone_id")
    void upsert(String id, String listId, String name, String status, String description,
                boolean isMilestone, String milestoneId);

    List<Task> findByListId(String listId);

    List<Task> findByMilestoneId(String milestoneId);

    List<Task> findByListIdAndIsMilestoneTrue(String listId);
}
