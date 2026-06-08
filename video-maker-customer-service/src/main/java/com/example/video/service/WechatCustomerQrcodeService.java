package com.example.video.service;

import com.example.video.exception.NotFoundException;
import com.example.video.model.WechatCustomerQrcode;
import com.example.video.repository.WechatCustomerQrcodeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WechatCustomerQrcodeService {

    private final WechatCustomerQrcodeRepository repository;

    public WechatCustomerQrcodeService(WechatCustomerQrcodeRepository repository) {
        this.repository = repository;
    }

    public List<WechatCustomerQrcode> listEnabled() {
        return repository.findByEnabledTrueOrderBySortOrderAsc();
    }

    public List<WechatCustomerQrcode> listAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder"));
    }

    public WechatCustomerQrcode getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("企业微信二维码不存在: " + id));
    }

    @Transactional
    public WechatCustomerQrcode create(WechatCustomerQrcode qrcode) {
        if (repository.existsByQrcodeKey(qrcode.getQrcodeKey())) {
            throw new IllegalArgumentException("二维码Key已存在: " + qrcode.getQrcodeKey());
        }
        return repository.save(qrcode);
    }

    @Transactional
    public WechatCustomerQrcode update(Long id, WechatCustomerQrcode qrcode) {
        WechatCustomerQrcode existing = getById(id);
        if (!existing.getQrcodeKey().equals(qrcode.getQrcodeKey())
                && repository.existsByQrcodeKeyAndIdNot(qrcode.getQrcodeKey(), id)) {
            throw new IllegalArgumentException("二维码Key已存在: " + qrcode.getQrcodeKey());
        }
        existing.setName(qrcode.getName());
        existing.setQrcodeUrl(qrcode.getQrcodeUrl());
        existing.setQrcodeKey(qrcode.getQrcodeKey());
        existing.setSortOrder(qrcode.getSortOrder());
        existing.setEnabled(qrcode.getEnabled());
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("企业微信二维码不存在: " + id);
        }
        repository.deleteById(id);
    }
}