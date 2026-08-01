package com.eduagent.code.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.code.dto.CodeExerciseRequest;
import com.eduagent.code.entity.CodeExercise;
import com.eduagent.code.mapper.CodeExerciseMapper;
import com.eduagent.code.service.CodeExerciseService;
import com.eduagent.code.vo.CodeExerciseVO;
import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.common.result.PageResult;
import com.eduagent.common.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CodeExerciseServiceImpl implements CodeExerciseService {

    private final CodeExerciseMapper mapper;

    @Override
    public CodeExerciseVO create(CodeExerciseRequest request) {
        // 身份来自网关注入的 X-User-*（AuthHeaderFilter 已写入 AuthContext）
        if (AuthContext.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "未认证");
        }
        CodeExercise exercise = new CodeExercise();
        exercise.setTitle(request.getTitle());
        exercise.setDescription(request.getDescription());
        exercise.setDifficulty(request.getDifficulty());
        exercise.setLanguage(request.getLanguage());
        exercise.setStatus(1);
        mapper.insert(exercise);
        return toVO(exercise);
    }

    @Override
    public PageResult<CodeExerciseVO> list(int page, int size) {
        Page<CodeExercise> p = mapper.selectPage(new Page<>(page, size), Wrappers.emptyWrapper());
        List<CodeExerciseVO> voList = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(voList, p.getTotal());
    }

    @Override
    public CodeExerciseVO get(Long id) {
        CodeExercise exercise = mapper.selectById(id);
        if (exercise == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "练习不存在");
        }
        return toVO(exercise);
    }

    private CodeExerciseVO toVO(CodeExercise e) {
        return new CodeExerciseVO(e.getId(), e.getTitle(), e.getDifficulty(),
                e.getLanguage(), e.getStatus(), e.getCreateTime());
    }
}
