package com.example.video.repository;

import com.example.video.model.TemplateConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateConfigRepository extends JpaRepository<TemplateConfig, Long> {
    Optional<TemplateConfig> findByTemplate_Id(Long templateId);
}
