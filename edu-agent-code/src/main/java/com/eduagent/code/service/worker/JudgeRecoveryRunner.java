package com.eduagent.code.service.worker;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.code.entity.CodeSubmission;
import com.eduagent.code.entity.SubmissionStatus;
import com.eduagent.code.mapper.CodeSubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动补偿：进程在上一轮挂了会留下 PENDING/RUNNING 的悬空提交，
 * 启动时把这些重新置 PENDING 并入队重判（方案 a 的可靠性兜底，幂等）。
 * 数据库未就绪时静默跳过，不阻断启动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeRecoveryRunner implements ApplicationRunner {

    private final CodeSubmissionMapper submissionMapper;
    private final JudgeWorker judgeWorker;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<CodeSubmission> stale = submissionMapper.selectList(new LambdaQueryWrapper<CodeSubmission>()
                    .in(CodeSubmission::getStatus, SubmissionStatus.PENDING, SubmissionStatus.RUNNING));
            if (stale.isEmpty()) {
                return;
            }
            log.warn("启动补偿：重判 {} 条遗留判分", stale.size());
            for (CodeSubmission s : stale) {
                s.setStatus(SubmissionStatus.PENDING);
                submissionMapper.updateById(s);
                judgeWorker.judge(s.getId());
            }
        } catch (Exception e) {
            log.warn("启动补偿执行失败（数据库可能未就绪），跳过: {}", e.toString());
        }
    }
}
