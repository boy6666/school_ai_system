package com.eduagent.service;

import com.eduagent.entity.Course;
import com.eduagent.entity.Resource;
import com.eduagent.repository.CourseRepository;
import com.eduagent.repository.ResourceRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class AdminResourceService {

    private final ResourceRepository resourceRepository;
    private final CourseRepository courseRepository;

    public AdminResourceService(ResourceRepository resourceRepository, CourseRepository courseRepository) {
        this.resourceRepository = resourceRepository;
        this.courseRepository = courseRepository;
    }

    public Page<Resource> getResourceList(String keyword, String type, String status, int page, int pageSize) {
        Specification<Resource> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String kw = "%" + keyword + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), kw),
                        cb.like(root.get("author"), kw),
                        cb.like(root.get("courseName"), kw)
                ));
            }
            if (StringUtils.hasText(type)) {
                predicates.add(cb.equal(root.get("type"), Resource.ResourceType.valueOf(type)));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), Resource.ResourceStatus.valueOf(status)));
            }

            query.orderBy(cb.desc(root.get("updateTime")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return resourceRepository.findAll(spec, PageRequest.of(page - 1, pageSize));
    }

    public Resource updateResourceStatus(Long id, String status) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));
        resource.setStatus(Resource.ResourceStatus.valueOf(status));
        return resourceRepository.save(resource);
    }

    public Map<String, Object> toResourceVO(Resource r) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", r.getId());
        vo.put("title", r.getTitle());
        vo.put("type", r.getType().name());
        vo.put("difficulty", r.getDifficulty().name());
        vo.put("author", r.getAuthor());
        vo.put("status", r.getStatus().name());
        vo.put("createTime", r.getCreateTime());
        vo.put("updateTime", r.getUpdateTime());
        vo.put("views", r.getViews());
        vo.put("courseName", r.getCourseName());
        return vo;
    }

    public Page<Course> getCourseList(String keyword, String status, int page, int pageSize) {
        Specification<Course> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String kw = "%" + keyword + "%";
                predicates.add(cb.or(
                        cb.like(root.get("courseName"), kw),
                        cb.like(root.get("courseCode"), kw)
                ));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), Course.CourseStatus.valueOf(status)));
            }

            query.orderBy(cb.desc(root.get("updateTime")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return courseRepository.findAll(spec, PageRequest.of(page - 1, pageSize));
    }

    public Course updateCourseStatus(String id, String status) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        course.setStatus(Course.CourseStatus.valueOf(status));
        return courseRepository.save(course);
    }

    public Map<String, Object> toCourseVO(Course c) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", c.getId());
        vo.put("name", c.getCourseName());
        vo.put("code", c.getCourseCode());
        vo.put("description", c.getDescription());
        vo.put("teacherId", c.getTeacherId());
        vo.put("difficulty", c.getDifficulty().name());
        vo.put("status", c.getStatus().name());
        vo.put("createTime", c.getCreateTime());
        vo.put("updateTime", c.getUpdateTime());
        return vo;
    }
}
