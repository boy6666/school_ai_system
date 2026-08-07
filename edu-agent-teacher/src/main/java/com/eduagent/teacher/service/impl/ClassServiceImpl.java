package com.eduagent.teacher.service.impl;

import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.common.security.AuthContext;
import com.eduagent.teacher.dto.CreateClassRequest;
import com.eduagent.teacher.dto.UpdateClassRequest;
import com.eduagent.teacher.entity.Classes;
import com.eduagent.teacher.entity.ClassStudent;
import com.eduagent.teacher.feign.LearningServiceClient;
import com.eduagent.teacher.mapper.ClassesMapper;
import com.eduagent.teacher.mapper.ClassStudentMapper;
import com.eduagent.teacher.service.ClassService;
import com.eduagent.teacher.vo.ClassStudentVO;
import com.eduagent.teacher.vo.ClassVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private final ClassesMapper classesMapper;
    private final ClassStudentMapper classStudentMapper;
    private final LearningServiceClient learningClient;

    @Override
    public ClassVO create(CreateClassRequest request) {
        Long teacherId = currentUserId();
        Classes clazz = new Classes();
        clazz.setName(request.getName());
        clazz.setCourse(request.getCourse());
        clazz.setSemester(request.getSemester());
        clazz.setTeacherId(teacherId);
        clazz.setStatus(1);
        classesMapper.insert(clazz);
        return toVO(clazz, 0);
    }

    @Override
    public List<ClassVO> list() {
        Long teacherId = currentUserId();
        List<Classes> own = classesMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Classes>()
                        .eq(Classes::getTeacherId, teacherId)
                        .orderByDesc(Classes::getId));
        List<ClassVO> result = new ArrayList<>();
        own.forEach(c -> {
            int cnt = classStudentMapper.selectStudentIds(c.getId()).size();
            result.add(toVO(c, cnt));
        });
        return result;
    }

    @Override
    public ClassVO get(Long id) {
        Classes c = requireOwner(id);
        int cnt = classStudentMapper.selectStudentIds(id).size();
        return toVO(c, cnt);
    }

    @Override
    public ClassVO update(Long id, UpdateClassRequest request) {
        Classes c = requireOwner(id);
        if (request.getName() != null) {
            c.setName(request.getName());
        }
        if (request.getCourse() != null) {
            c.setCourse(request.getCourse());
        }
        if (request.getSemester() != null) {
            c.setSemester(request.getSemester());
        }
        classesMapper.updateById(c);
        return toVO(c, classStudentMapper.selectStudentIds(id).size());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireOwner(id);
        // 逻辑删班级（BaseEntity.deleted 全局生效）；成员关系表保留可查历史，不级联删。
        classesMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void addStudent(Long classId, Long studentId) {
        requireOwner(classId);
        if (classStudentMapper.exists(classId, studentId) > 0) {
            throw new ApiException(ErrorCode.CONFLICT, "该学生已在班级中");
        }
        classStudentMapper.insert(classId, studentId);
        // 回写 learning 侧学生 class_id（best-effort：learning 未就绪时仅告警，不阻塞）
        try {
            learningClient.bindClass(studentId, Map.of("classId", classId));
        } catch (Exception e) {
            log.warn("回写 learning student_profiles.class_id 失败 studentId={} classId={}: {}",
                    studentId, classId, e.getMessage());
        }
    }

    @Override
    public void removeStudent(Long classId, Long studentId) {
        requireOwner(classId);
        classStudentMapper.deleteRelation(classId, studentId);
    }

    @Override
    public List<ClassStudentVO> listStudents(Long classId) {
        requireOwner(classId);
        List<ClassStudent> relations = classStudentMapper.selectByClass(classId);
        List<ClassStudentVO> result = new ArrayList<>();
        relations.forEach(r -> result.add(new ClassStudentVO(r.getStudentId(), null, r.getJoinedAt())));
        return result;
    }

    /** 当前登录用户 id（网关注入），为空即未认证 */
    private Long currentUserId() {
        String uid = AuthContext.getUserId();
        if (uid == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "未认证");
        }
        return Long.valueOf(uid);
    }

    /** 校验班级存在且属于当前教师，返回实体 */
    private Classes requireOwner(Long id) {
        Long teacherId = currentUserId();
        Classes c = classesMapper.selectById(id);
        if (c == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "班级不存在");
        }
        if (!c.getTeacherId().equals(teacherId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权操作他人班级");
        }
        return c;
    }

    private ClassVO toVO(Classes c, int studentCount) {
        return new ClassVO(c.getId(), c.getName(), c.getTeacherId(), c.getCourse(),
                c.getSemester(), c.getStatus(), c.getCreateTime(), studentCount);
    }
}
