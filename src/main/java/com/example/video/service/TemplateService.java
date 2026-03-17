package com.example.video.service;

import com.example.video.dto.OptionView;
import com.example.video.dto.TemplateView;
import com.example.video.exception.NotFoundException;
import com.example.video.model.VideoTemplate;
import com.example.video.repository.VideoTemplateRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TemplateService {

    private static final List<OptionMeta> THEME_OPTIONS = Arrays.asList(
            new OptionMeta("all", "全部"),
            new OptionMeta("screen", "生日投屏"),
            new OptionMeta("study", "升学"),
            new OptionMeta("housewarming", "乔迁"),
            new OptionMeta("birthday", "生日祝福"),
            new OptionMeta("hero", "奥特曼")
    );

    private static final List<OptionMeta> CATEGORY_OPTIONS = Arrays.asList(
            new OptionMeta("all", "综合"),
            new OptionMeta("invitation", "邀请函"),
            new OptionMeta("tv", "电视投屏图"),
            new OptionMeta("blessing", "祝福视频")
    );

    private final VideoTemplateRepository templateRepository;

    public TemplateService(VideoTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<OptionView> listThemes() {
        List<VideoTemplate> templates = templateRepository.findAllByEnabledTrueOrderBySortOrderAscIdDesc();
        Map<String, Long> counts = templates.stream()
                .collect(Collectors.groupingBy(VideoTemplate::getThemeKey, Collectors.counting()));
        return toOptionViews(THEME_OPTIONS, counts, templates.size());
    }

    public List<OptionView> listCategories() {
        List<VideoTemplate> templates = templateRepository.findAllByEnabledTrueOrderBySortOrderAscIdDesc();
        Map<String, Long> counts = templates.stream()
                .collect(Collectors.groupingBy(VideoTemplate::getCategoryKey, Collectors.counting()));
        return toOptionViews(CATEGORY_OPTIONS, counts, templates.size());
    }

    public List<TemplateView> listTemplates(String keyword, String theme, String category) {
        Specification<VideoTemplate> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("enabled")));

            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(theme) && !"all".equalsIgnoreCase(theme)) {
                predicates.add(cb.equal(root.get("themeKey"), theme));
            }
            if (StringUtils.hasText(category) && !"all".equalsIgnoreCase(category)) {
                predicates.add(cb.equal(root.get("categoryKey"), category));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.desc("id"));
        return templateRepository.findAll(specification, sort)
                .stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    public TemplateView getTemplate(Long templateId) {
        return toView(findEntityById(templateId));
    }

    public VideoTemplate findEntityById(Long templateId) {
        return templateRepository.findByIdAndEnabledTrue(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));
    }

    public TemplateView toView(VideoTemplate template) {
        TemplateView view = new TemplateView();
        view.setId(template.getId());
        view.setTemplateCode(template.getTemplateCode());
        view.setName(template.getName());
        view.setSubtitle(template.getSubtitle());
        view.setDescription(template.getDescription());
        view.setTheme(template.getThemeKey());
        view.setThemeName(template.getThemeName());
        view.setCategory(template.getCategoryKey());
        view.setCategoryName(template.getCategoryName());
        view.setTag(template.getTagName());
        view.setPrice(template.getPrice());
        view.setColors(Arrays.asList(defaultColor(template.getColorStart()), defaultColor(template.getColorEnd())));
        view.setCoverUrl(template.getCoverUrl());
        view.setPreviewUrl(template.getPreviewUrl());
        view.setInventoryCount(template.getInventoryCount());
        view.setSalesCount(template.getSalesCount());
        view.setEnabled(template.getEnabled());
        view.setSortOrder(template.getSortOrder());
        return view;
    }

    private String defaultColor(String color) {
        return StringUtils.hasText(color) ? color : "#c90b18";
    }

    private List<OptionView> toOptionViews(List<OptionMeta> source, Map<String, Long> counts, long totalCount) {
        return source.stream()
                .map(meta -> {
                    long count = "all".equals(meta.getKey()) ? totalCount : counts.getOrDefault(meta.getKey(), 0L);
                    return new OptionView(meta.getKey(), meta.getName(), count);
                })
                .collect(Collectors.toList());
    }

    private static class OptionMeta {
        private final String key;
        private final String name;

        private OptionMeta(String key, String name) {
            this.key = key;
            this.name = name;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }
    }
}