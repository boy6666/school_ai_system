<template>
  <div class="code-practice-page">
    <el-card>
      <template #header>
        <div class="page-header">
          <div>
            <h2>代码练习</h2>
            <p>提交 Java 代码后，可根据提交编号查询判分结果。</p>
          </div>
        </div>
      </template>

      <el-form label-width="110px">
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item label="编程语言">
              <el-input model-value="Java" disabled />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :md="12">
            <el-form-item label="入口类名" required>
              <el-input
                v-model="className"
                placeholder="例如 Main"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :md="12">
            <el-form-item label="作业编号">
              <el-input-number
                v-model="assignmentId"
                :min="1"
                controls-position="right"
                placeholder="选填"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :md="12">
            <el-form-item label="作业项编号">
              <el-input-number
                v-model="assignmentItemId"
                :min="1"
                controls-position="right"
                placeholder="选填"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="预期输出">
              <el-input
                v-model="expectedOutput"
                type="textarea"
                :rows="3"
                placeholder="选填，用于输出比对"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="源代码" required>
          <CodeEditor
            v-model="sourceCode"
            language="java"
            height="420px"
            :readonly="false"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            @click="handleSubmit"
          >
            提交判分
          </el-button>

          <el-button
            v-if="result"
            :loading="querying"
            @click="refreshResult"
          >
            查询最新结果
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result" class="result-card">
      <template #header>
        <div class="result-header">
          <span>判分结果</span>
          <el-tag :type="statusTagType(result.status)">
            {{ statusText(result.status) }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="提交编号">
          {{ result.submissionId }}
        </el-descriptions-item>

        <el-descriptions-item label="总分">
          {{ result.overallScore ?? '暂未返回' }}
        </el-descriptions-item>

        <el-descriptions-item label="编译状态">
          {{ compileText }}
        </el-descriptions-item>

        <el-descriptions-item label="运行时间">
          {{
            result.runTimeMs == null
              ? '暂未返回'
              : `${result.runTimeMs} ms`
          }}
        </el-descriptions-item>
      </el-descriptions>

      <section class="result-section">
        <h3>运行输出</h3>
        <pre>{{ result.stdout || '暂无输出' }}</pre>
      </section>

      <section class="result-section">
        <h3>Checkstyle 检查</h3>
        <pre>{{ result.checkstyle || '暂未返回' }}</pre>
      </section>

      <section class="result-section">
        <h3>PMD 检查</h3>
        <pre>{{ result.pmd || '暂未返回' }}</pre>
      </section>

      <section class="result-section">
        <h3>AI 建议</h3>
        <p>{{ result.aiSuggestion || '暂未返回' }}</p>
      </section>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import CodeEditor from '@/components/CodeEditor.vue'
import {
  getCodeResult,
  submitCode,
  type CodeSubmitResult,
  type CodeSubmitStatus
} from '@/api/code'

type TagType =
  | 'success'
  | 'warning'
  | 'danger'
  | 'info'

const route = useRoute()

function queryId(value: unknown): number | undefined {
  const raw = Array.isArray(value) ? value[0] : value
  const parsed = Number(raw)

  return Number.isInteger(parsed) && parsed > 0
    ? parsed
    : undefined
}

const className = ref('Main')
const sourceCode = ref('')
const expectedOutput = ref('')
const assignmentId = ref<number | undefined>(
  queryId(route.query.assignmentId)
)
const assignmentItemId = ref<number | undefined>(
  queryId(route.query.assignmentItemId)
)

const submitting = ref(false)
const querying = ref(false)
const result = ref<CodeSubmitResult>()

const compileText = computed(() => {
  if (result.value?.compileOk === 1) return '通过'
  if (result.value?.compileOk === 0) return '未通过'
  return '暂未返回'
})

function statusText(status: CodeSubmitStatus): string {
  const labels: Record<CodeSubmitStatus, string> = {
    0: '等待处理',
    1: '运行中',
    2: '判分完成',
    3: '运行超时',
    4: '编译失败',
    5: '判分失败'
  }

  return labels[status]
}

function statusTagType(status: CodeSubmitStatus): TagType {
  if (status === 2) return 'success'
  if (status === 0 || status === 1) return 'warning'
  return 'danger'
}

async function handleSubmit() {
  if (!className.value.trim()) {
    ElMessage.warning('请输入入口类名')
    return
  }

  if (!sourceCode.value.trim()) {
    ElMessage.warning('请输入源代码')
    return
  }

  submitting.value = true

  try {
    const receipt = await submitCode({
      assignmentId: assignmentId.value,
      assignmentItemId: assignmentItemId.value,
      language: 'java',
      className: className.value.trim(),
      sourceCode: sourceCode.value,
      expectedOutput:
        expectedOutput.value.trim() || undefined,
      mode: 'IO'
    })

    result.value = receipt
    ElMessage.success(
      '代码已受理，请点击“查询最新结果”查看判分进度'
    )
  } catch {
    // 公共请求层统一显示正式接口错误
  } finally {
    submitting.value = false
  }
}

async function refreshResult() {
  if (!result.value) return

  querying.value = true

  try {
    result.value = await getCodeResult(
      result.value.submissionId
    )
  } catch {
    // 公共请求层统一显示正式接口错误
  } finally {
    querying.value = false
  }
}
</script>

<style scoped>
.code-practice-page {
  padding: 20px;
}

.page-header h2 {
  margin: 0;
}

.page-header p {
  margin: 8px 0 0;
  color: #909399;
}

.result-card {
  margin-top: 20px;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result-section {
  margin-top: 20px;
}

.result-section h3 {
  margin-bottom: 10px;
  font-size: 16px;
}

.result-section pre,
.result-section p {
  box-sizing: border-box;
  margin: 0;
  padding: 14px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f5f7fa;
  border-radius: 6px;
}
</style>