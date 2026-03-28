package com.example.video.repository;

import com.example.video.model.WechatCustomerQrcode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WechatCustomerQrcodeRepository extends JpaRepository<WechatCustomerQrcode, Long> {

    List<WechatCustomerQrcode> findByEnabledTrueOrderBySortOrderAsc();

    Optional<WechatCustomerQrcode> findByQrcodeKey(String qrcodeKey);

    boolean existsByQrcodeKey(String qrcodeKey);

    boolean existsByQrcodeKeyAndIdNot(String qrcodeKey, Long id);
}