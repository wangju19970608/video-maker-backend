package com.example.video.repository;

import com.example.video.model.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByOpenid(String openid);

    Optional<SysUser> findByUnionid(String unionid);

    boolean existsByOpenid(String openid);
}