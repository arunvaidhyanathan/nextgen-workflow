package com.flowable.wrapper.repository;

import com.flowable.wrapper.entity.QueueTask;
import com.flowable.wrapper.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QueueTaskRepository extends JpaRepository<QueueTask, String> {
    
    // Find tasks by queue name
    List<QueueTask> findByQueueNameAndStatusOrderByPriorityDescCreatedAtAsc(String queueName, TaskStatus status);
    
    // Find unassigned tasks by queue name
    List<QueueTask> findByQueueNameAndStatusAndAssigneeIsNullOrderByPriorityDescCreatedAtAsc(String queueName, TaskStatus status);
    
    // Find tasks by assignee
    List<QueueTask> findByAssigneeAndStatusInOrderByPriorityDescCreatedAtAsc(String assignee, List<TaskStatus> statuses);
    
    // Find tasks by process instance
    List<QueueTask> findByProcessInstanceIdAndStatusOrderByCreatedAtAsc(String processInstanceId, TaskStatus status);
    
    
    // Find tasks by queue with pagination
    Page<QueueTask> findByQueueNameAndStatus(String queueName, TaskStatus status, Pageable pageable);
    
    // Find unassigned tasks by queue with pagination
    Page<QueueTask> findByQueueNameAndStatusAndAssigneeIsNull(String queueName, TaskStatus status, Pageable pageable);
    
    // Find task by task ID (for Cerbos integration)
    Optional<QueueTask> findByTaskId(String taskId);
    
    // Find all tasks by process instance ID (for Cerbos integration)
    List<QueueTask> findByProcessInstanceId(String processInstanceId);
    
}