package com.example.camunda.repository;

import com.example.camunda.model.JobHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobHistoryRepository extends JpaRepository<JobHistory, Long> {
    
    long countByExecutionTimeAfter(LocalDateTime startTime);
    
    @Query("SELECT j.jobType, COUNT(j) FROM JobHistory j GROUP BY j.jobType")
    List<Object[]> countJobsByTypeRaw();
}
