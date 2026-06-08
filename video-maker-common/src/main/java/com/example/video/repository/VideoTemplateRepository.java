package com.example.video.repository;

import com.example.video.model.VideoTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface VideoTemplateRepository extends JpaRepository<VideoTemplate, Long>, JpaSpecificationExecutor<VideoTemplate> {

    Optional<VideoTemplate> findByIdAndEnabledTrue(Long id);

    Optional<VideoTemplate> findByTemplateCode(String templateCode);

    List<VideoTemplate> findAllByEnabledTrueOrderBySortOrderAscIdDesc();

    long countByEnabledTrue();
}