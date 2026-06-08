package com.example.video.admin.repository;

import com.example.video.admin.model.AdminMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminMenuRepository extends JpaRepository<AdminMenu, Long> {

    List<AdminMenu> findAllByOrderBySortOrderAscIdAsc();

    List<AdminMenu> findByStatusOrderBySortOrderAscIdAsc(Integer status);
}