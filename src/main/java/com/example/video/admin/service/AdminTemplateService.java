package com.example.video.admin.service;

import com.example.video.admin.dto.TemplateUpsertRequest;
import com.example.video.dto.TemplateView;
import com.example.video.exception.NotFoundException;
import com.example.video.model.VideoTemplate;
import com.example.video.repository.OrderRecordRepository;
import com.example.video.repository.VideoTemplateRepository;
import com.example.video.service.TemplateService;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminTemplateService {

    private final VideoTemplateRepository templateRepository;
    private final OrderRecordRepository orderRepository;
    private final TemplateService templateService;

    public AdminTemplateService(VideoTemplateRepository templateRepository,
                                OrderRecordRepository orderRepository,
                                TemplateService templateService) {
        this.templateRepository = templateRepository;
        this.orderRepository = orderRepository;
        this.templateService = templateService;
    }

    public List<TemplateView> listTemplates(String keyword, String theme, String category, Boolean enabled) {
        Specification<VideoTemplate> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase().trim() + "%"));
            }
            if (StringUtils.hasText(theme) && !"all".equalsIgnoreCase(theme)) {
                predicates.add(cb.equal(root.get("themeKey"), theme));
            }
            if (StringUtils.hasText(category) && !"all".equalsIgnoreCase(category)) {
                predicates.add(cb.equal(root.get("categoryKey"), category));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.desc("id"));
        return templateRepository.findAll(specification, sort).stream()
                .map(templateService::toView)
                .collect(Collectors.toList());
    }

    public TemplateView getTemplate(Long templateId) {
        VideoTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));
        return templateService.toView(template);
    }

    @Transactional
    public TemplateView createTemplate(TemplateUpsertRequest request) {
        validateRequest(request, true);

        String code = request.getTemplateCode().trim();
        if (templateRepository.findByTemplateCode(code).isPresent()) {
            throw new IllegalArgumentException("templateCode already exists");
        }

        VideoTemplate template = new VideoTemplate();
        fillTemplate(template, request, true);
        template.setSalesCount(0L);

        return templateService.toView(templateRepository.save(template));
    }

    @Transactional
    public TemplateView updateTemplate(Long templateId, TemplateUpsertRequest request) {
        validateRequest(request, false);

        VideoTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));

        if (StringUtils.hasText(request.getTemplateCode())) {
            String code = request.getTemplateCode().trim();
            templateRepository.findByTemplateCode(code)
                    .filter(other -> !other.getId().equals(templateId))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("templateCode already exists");
                    });
        }

        fillTemplate(template, request, false);
        return templateService.toView(templateRepository.save(template));
    }

    @Transactional
    public TemplateView updateStatus(Long templateId, Boolean enabled) {
        VideoTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));
        template.setEnabled(enabled != null && enabled);
        return templateService.toView(templateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(Long templateId) {
        VideoTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));

        long orderCount = orderRepository.countByTemplate_Id(templateId);
        if (orderCount > 0) {
            template.setEnabled(false);
            templateRepository.save(template);
            return;
        }

        templateRepository.delete(template);
    }

    @Transactional
    public TemplateView updatePreviewUrl(Long templateId, String relativePath) {
        VideoTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));
        template.setPreviewUrl(relativePath);
        // video 类型文件自动设置 templateType
        if (relativePath != null && relativePath.endsWith(".mp4")) {
            template.setTemplateType("video");
        }
        return templateService.toView(templateRepository.save(template));
    }

    private void fillTemplate(VideoTemplate template, TemplateUpsertRequest request, boolean createMode) {
        if (StringUtils.hasText(request.getTemplateCode())) {
            template.setTemplateCode(request.getTemplateCode().trim());
        } else if (createMode) {
            throw new IllegalArgumentException("templateCode is required");
        }

        if (StringUtils.hasText(request.getName())) {
            template.setName(request.getName().trim());
        } else if (createMode) {
            throw new IllegalArgumentException("name is required");
        }

        if (StringUtils.hasText(request.getThemeKey())) {
            template.setThemeKey(request.getThemeKey().trim());
        } else if (createMode) {
            throw new IllegalArgumentException("themeKey is required");
        }

        if (StringUtils.hasText(request.getThemeName())) {
            template.setThemeName(request.getThemeName().trim());
        } else if (createMode) {
            throw new IllegalArgumentException("themeName is required");
        }

        if (StringUtils.hasText(request.getCategoryKey())) {
            template.setCategoryKey(request.getCategoryKey().trim());
        } else if (createMode) {
            throw new IllegalArgumentException("categoryKey is required");
        }

        if (StringUtils.hasText(request.getCategoryName())) {
            template.setCategoryName(request.getCategoryName().trim());
        } else if (createMode) {
            throw new IllegalArgumentException("categoryName is required");
        }

        if (request.getPrice() != null) {
            template.setPrice(request.getPrice());
        } else if (createMode) {
            throw new IllegalArgumentException("price is required");
        }

        if (request.getSubtitle() != null) {
            template.setSubtitle(trimToNull(request.getSubtitle()));
        }
        if (request.getDescription() != null) {
            template.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getTagName() != null) {
            template.setTagName(trimToNull(request.getTagName()));
        }
        if (request.getColorStart() != null) {
            template.setColorStart(defaultColor(request.getColorStart().trim(), "#c90b18"));
        } else if (createMode) {
            template.setColorStart("#c90b18");
        }
        if (request.getColorEnd() != null) {
            template.setColorEnd(defaultColor(request.getColorEnd().trim(), "#81000f"));
        } else if (createMode) {
            template.setColorEnd("#81000f");
        }
        if (request.getCoverUrl() != null) {
            template.setCoverUrl(trimToNull(request.getCoverUrl()));
        }
        if (request.getPreviewUrl() != null) {
            template.setPreviewUrl(trimToNull(request.getPreviewUrl()));
        }
        if (request.getInventoryCount() != null) {
            template.setInventoryCount(request.getInventoryCount());
        } else if (createMode && template.getInventoryCount() == null) {
            template.setInventoryCount(99999);
        }
        if (request.getSortOrder() != null) {
            template.setSortOrder(request.getSortOrder());
        }
        if (request.getEnabled() != null) {
            template.setEnabled(request.getEnabled());
        }
        if (template.getSalesCount() == null) {
            template.setSalesCount(0L);
        }
        if (template.getPrice() == null) {
            template.setPrice(BigDecimal.ZERO);
        }
    }

    private String defaultColor(String color, String fallback) {
        if (!StringUtils.hasText(color)) {
            return fallback;
        }
        return color.startsWith("#") ? color : "#" + color;
    }

    private String trimToNull(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private void validateRequest(TemplateUpsertRequest request, boolean createMode) {
        if (createMode && !StringUtils.hasText(request.getTemplateCode())) {
            throw new IllegalArgumentException("templateCode is required");
        }
        if (createMode && !StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("name is required");
        }
    }
}