package com.eduagent.code.service.score;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 判分权重矩阵（dev-wuyoucheng §2.4.6）：
 * compile 40 / checkstyle error -3 warning -1(下限-20) / pmd -3(下限-20) / run 完成+40 输出匹配+20，封顶100底0。
 */
class ScoreServiceTest {

    private final ScoreService scoreService = new ScoreService();

    @Test
    void perfectSubmissionScores100() {
        assertThat(scoreService.score(true, 0, 0, 0, true, true).score()).isEqualTo(100);
    }

    @Test
    void compileFailScoresZero() {
        ScoreResult r = scoreService.score(false, 0, 0, 0, false, false);
        assertThat(r.score()).isZero();
        assertThat(r.detail().get("run")).isZero();
    }

    @Test
    void checkstylePenalty() {
        assertThat(scoreService.score(true, 1, 0, 0, true, true).score()).isEqualTo(97);
        assertThat(scoreService.score(true, 0, 1, 0, true, true).score()).isEqualTo(99);
    }

    @Test
    void pmdPenalty() {
        assertThat(scoreService.score(true, 0, 0, 1, true, true).score()).isEqualTo(97);
    }

    @Test
    void runNotPassedGetsNoRunPoints() {
        // 运行未完成/崩溃：不加运行分也不加输出分
        assertThat(scoreService.score(true, 0, 0, 0, false, false).score()).isEqualTo(40);
    }

    @Test
    void runPassedButOutputMismatch() {
        assertThat(scoreService.score(true, 0, 0, 0, true, false).score()).isEqualTo(80);
    }

    @Test
    void penaltiesFlooredAtTwenty() {
        assertThat(scoreService.score(true, 100, 0, 0, true, true).score()).isEqualTo(80);
        assertThat(scoreService.score(true, 0, 0, 100, true, true).score()).isEqualTo(80);
    }

    @Test
    void floorZero() {
        // 编译40分被 checkstyle/pmd 各自 20 的上限罚分扣平：40 - 20 - 20 = 0
        assertThat(scoreService.score(true, 100, 0, 100, false, false).score()).isZero();
    }
}
