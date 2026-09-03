package com.eduagent.resource.service;

import com.eduagent.resource.vo.ResourceVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHAPTER_CACHE_PREFIX = "resource:chapter:";
    private static final String DETAIL_CACHE_PREFIX = "resource:detail:";

    // === 章节资源缓存（30分钟） ===
    public String buildChapterKey(String chapterId, String type) {
        return CHAPTER_CACHE_PREFIX + chapterId + ":" + type;
    }

    public void cacheChapterResources(String chapterId, String type, List<ResourceVO> resources) {
        try {
            String key = buildChapterKey(chapterId, type);
            String json = objectMapper.writeValueAsString(resources);
            redisTemplate.opsForValue().set(key, json, 30, TimeUnit.MINUTES);
            log.debug("缓存章节资源: chapterId={}, type={}, count={}", chapterId, type, resources.size());
        } catch (Exception e) {
            log.warn("缓存章节资源失败: chapterId={}, type={}", chapterId, type, e);
        }
    }

    public List<ResourceVO> getCachedChapterResources(String chapterId, String type) {
        try {
            String key = buildChapterKey(chapterId, type);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ResourceVO.class));
            }
        } catch (Exception e) {
            log.debug("读取章节缓存失败: chapterId={}, type={}", chapterId, type, e);
        }
        return null;
    }

    public void evictChapterCache(String chapterId, String type) {
        try {
            String key = buildChapterKey(chapterId, type);
            redisTemplate.delete(key);
            log.debug("清除章节缓存: chapterId={}, type={}", chapterId, type);
        } catch (Exception e) {
            log.warn("清除章节缓存失败: chapterId={}, type={}", chapterId, type, e);
        }
    }

    // === 详情缓存（10分钟） ===
    public String buildDetailKey(Long resourceId) {
        return DETAIL_CACHE_PREFIX + resourceId;
    }

    public void cacheResourceDetail(Long resourceId, ResourceVO resource) {
        try {
            String key = buildDetailKey(resourceId);
            String json = objectMapper.writeValueAsString(resource);
            redisTemplate.opsForValue().set(key, json, 10, TimeUnit.MINUTES);
            log.debug("缓存资源详情: id={}", resourceId);
        } catch (Exception e) {
            log.warn("缓存资源详情失败: id={}", resourceId, e);
        }
    }

    public ResourceVO getCachedResourceDetail(Long resourceId) {
        try {
            String key = buildDetailKey(resourceId);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, ResourceVO.class);
            }
        } catch (Exception e) {
            log.debug("读取详情缓存失败: id={}", resourceId, e);
        }
        return null;
    }

    public void evictDetailCache(Long resourceId) {
        try {
            String key = buildDetailKey(resourceId);
            redisTemplate.delete(key);
            log.debug("清除详情缓存: id={}", resourceId);
        } catch (Exception e) {
            log.warn("清除详情缓存失败: id={}", resourceId, e);
        }
    }

    public void evictAllCaches(Long resourceId, String chapterId, String type) {
        if (resourceId != null) {
            evictDetailCache(resourceId);
        }
        if (chapterId != null && type != null) {
            evictChapterCache(chapterId, type);
        }
        // 为了保险，也删除通配的章节缓存（但这里不实现批量删除，因为Redis不支持通配删除）
    }
}
