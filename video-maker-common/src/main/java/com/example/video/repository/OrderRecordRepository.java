package com.example.video.repository;

import com.example.video.model.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRecordRepository extends JpaRepository<OrderRecord, Long>, JpaSpecificationExecutor<OrderRecord> {

    List<OrderRecord> findAllByOrderByCreatedAtDesc();

    Optional<OrderRecord> findByOrderNo(String orderNo);

    long countByStatus(String status);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByStatusAndCreatedAtBetween(String status, LocalDateTime start, LocalDateTime end);

    long countByTemplate_Id(Long templateId);

    @Query("select coalesce(sum(o.amount), 0) from OrderRecord o where o.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") String status);

    @Query("select coalesce(sum(o.amount), 0) from OrderRecord o where o.status = :status and o.createdAt between :start and :end")
    BigDecimal sumAmountByStatusAndCreatedAtBetween(@Param("status") String status,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    List<OrderRecord> findAllByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<OrderRecord> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}