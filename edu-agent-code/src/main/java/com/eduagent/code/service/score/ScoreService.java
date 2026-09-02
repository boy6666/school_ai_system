package com.eduagent.code.service.score;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 判分权重（dev-wuyoucheng §2.4.6，常量集中便于调参）：
 * <pre>
 * compile(40)  : 编译通过 +40，否则 0（且 status=4 直接结束，不进沙箱）
 * checkstyle   : 每 error -3，每 warning -1（下限 -20）
 * pmd          : 每 violation -3（下限 -20）
 * run(60)      : 编译+检查通过才运行；运行完成(exit 0) +40，stdout==expectedOutput 再 +20
 * AI(参考)     : 不加减分，仅写入 ai_suggestion
 * </pre>
 * 封顶 100、底 0。「运行完成」以退出码 0 计（崩溃程序不给满运行分）。
 */
@Service
public class ScoreService {

    public static final int COMPILE_WEIGHT = 40;
    public static final int RUN_WEIGHT = 40;
    public static final int OUTPUT_WEIGHT = 20;
    public static final int CHECKSTYLE_ERROR_PENALTY = 3;
    public static final int CHECKSTYLE_WARNING_PENALTY = 1;
    public static final int PMD_PENALTY = 3;
    public static final int PENALTY_FLOOR = -20;

    /**
     * @param compileOk        编译是否通过
     * @param checkstyleErrors Checkstyle error 数
     * @param checkstyleWarns  Checkstyle warning 数
     * @param pmdCount         PMD violation 数
     * @param runPassed        运行完成且 exit 0
     * @param outputMatched    运行输出与期望输出一致（仅在 runPassed 时计）
     */
    public ScoreResult score(boolean compileOk,
                             int checkstyleErrors,
                             int checkstyleWarns,
                             int pmdCount,
                             boolean runPassed,
                             boolean outputMatched) {
        Map<String, Integer> detail = new LinkedHashMap<>();
        if (!compileOk) {
            detail.put("compile", 0);
            detail.put("checkstyle", 0);
            detail.put("pmd", 0);
            detail.put("run", 0);
            detail.put("total", 0);
            return new ScoreResult(0, detail);
        }

        int runPts = 0;
        if (runPassed) {
            runPts += RUN_WEIGHT;
            if (outputMatched) {
                runPts += OUTPUT_WEIGHT;
            }
        }

        int checkPts = Math.max(PENALTY_FLOOR,
                -(checkstyleErrors * CHECKSTYLE_ERROR_PENALTY + checkstyleWarns * CHECKSTYLE_WARNING_PENALTY));
        int pmdPts = Math.max(PENALTY_FLOOR, -(pmdCount * PMD_PENALTY));

        int total = clamp(COMPILE_WEIGHT + runPts + checkPts + pmdPts, 0, 100);

        detail.put("compile", COMPILE_WEIGHT);
        detail.put("checkstyle", checkPts);
        detail.put("pmd", pmdPts);
        detail.put("run", runPts);
        detail.put("total", total);
        return new ScoreResult(total, detail);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
