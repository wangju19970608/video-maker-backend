package com.example.video.repository;

import com.example.video.entity.DyVideoExtractorTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DyVideoExtractorTaskRepository extends JpaRepository<DyVideoExtractorTask, Long> {
    List<DyVideoExtractorTask> findByUserIdOrderByCreatedAtDesc(Long userId);
}
