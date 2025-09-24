package com.example.camunda.repository;

import com.example.camunda.model.JobHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;

@Repository
public interface JobHistoryRepository extends JpaRepository<JobHistory, Long> {
    
    long countByExecutionTimeAfter(LocalDateTime startTime);
    
    @Query("SELECT j.jobType as jobType, COUNT(j) as count FROM JobHistory j GROUP BY j.jobType")
    Map<String, Long> countJobsByType();
}
