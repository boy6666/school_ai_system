package com.eduagent.teacher.service;

import com.eduagent.teacher.dto.CreateAssignmentRequest;
import com.eduagent.teacher.dto.UpdateAssignmentRequest;
import com.eduagent.teacher.vo.AssignmentDetailVO;
import com.eduagent.teacher.vo.AssignmentVO;

import java.util.List;

public interface AssignmentService {

    AssignmentVO create(CreateAssignmentRequest request);

    List<AssignmentVO> list(Long classId);

    AssignmentDetailVO getDetail(Long id);

    AssignmentVO update(Long id, UpdateAssignmentRequest request);

    void delete(Long id);

    AssignmentVO addItem(Long id, CreateAssignmentRequest.ItemReq item);

    AssignmentVO publish(Long id);
}
