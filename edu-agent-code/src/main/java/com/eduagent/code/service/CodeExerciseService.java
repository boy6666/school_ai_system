package com.eduagent.code.service;

import com.eduagent.code.dto.CodeExerciseRequest;
import com.eduagent.code.vo.CodeExerciseVO;
import com.eduagent.common.result.PageResult;

public interface CodeExerciseService {

    CodeExerciseVO create(CodeExerciseRequest request);

    PageResult<CodeExerciseVO> list(int page, int size);

    CodeExerciseVO get(Long id);
}
