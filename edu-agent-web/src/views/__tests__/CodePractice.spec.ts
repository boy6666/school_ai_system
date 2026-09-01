import {
  flushPromises,
  mount
} from '@vue/test-utils'
import ElementPlus from 'element-plus'
import {
  beforeEach,
  describe,
  expect,
  it,
  vi
} from 'vitest'
import CodePractice from '@/views/student/CodePractice.vue'

const {
  getCodeResultMock,
  submitCodeMock
} = vi.hoisted(() => ({
  getCodeResultMock: vi.fn(),
  submitCodeMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: {}
  })
}))

vi.mock('@/api/code', () => ({
  submitCode: submitCodeMock,
  getCodeResult: getCodeResultMock
}))
vi.mock('@/components/CodeEditor.vue', () => ({
  default: {
    name: 'CodeEditor',
    props: [
      'modelValue',
      'language',
      'height',
      'readonly'
    ],
    emits: ['update:modelValue'],
    template: `
      <textarea
        data-testid="code-editor"
        :value="modelValue"
        @input="$emit(
          'update:modelValue',
          $event.target.value
        )"
      />
    `
  }
}))

const CodeEditorStub = {
  name: 'CodeEditor',
  props: [
    'modelValue',
    'language',
    'height',
    'readonly'
  ],
  emits: ['update:modelValue'],
  template: `
    <textarea
      data-testid="code-editor"
      :value="modelValue"
      @input="$emit(
        'update:modelValue',
        $event.target.value
      )"
    />
  `
}

function mountPage() {
  return mount(CodePractice, {
    global: {
      plugins: [ElementPlus],
      stubs: {
        CodeEditor: CodeEditorStub
      }
    }
  })
}

describe('学生代码判分页面', () => {
  beforeEach(() => {
    submitCodeMock.mockReset()
    getCodeResultMock.mockReset()
  })

  it('源代码为空时不应提交请求', async () => {
    const wrapper = mountPage()
    const submitButton = wrapper
      .findAll('button')
      .find(button =>
        button.text().includes('提交判分')
      )

    expect(submitButton).toBeDefined()

    await submitButton!.trigger('click')

    expect(submitCodeMock).not.toHaveBeenCalled()
  })

  it('应提交代码并查询展示正式判分结果', async () => {
    submitCodeMock.mockResolvedValue({
      submissionId: 1024,
      status: 0
    })

    getCodeResultMock.mockResolvedValue({
      submissionId: 1024,
      status: 2,
      stdout: 'Hello EduAgent',
      runTimeMs: 120,
      compileOk: 1,
      checkstyle: '{}',
      pmd: '{}',
      aiSuggestion: '代码结构清晰',
      overallScore: 95
    })

    const wrapper = mountPage()
    const sourceCode =
      'public class Main {' +
      ' public static void main(String[] args) {}' +
      ' }'

    await wrapper
      .get('[data-testid="code-editor"]')
      .setValue(sourceCode)

    const submitButton = wrapper
      .findAll('button')
      .find(button =>
        button.text().includes('提交判分')
      )

    await submitButton!.trigger('click')
    await flushPromises()

    expect(submitCodeMock).toHaveBeenCalledWith({
      assignmentId: undefined,
      assignmentItemId: undefined,
      language: 'java',
      className: 'Main',
      sourceCode,
      expectedOutput: undefined,
      mode: 'IO'
    })

    expect(wrapper.text()).toContain('1024')

    const queryButton = wrapper
      .findAll('button')
      .find(button =>
        button.text().includes('查询最新结果')
      )

    expect(queryButton).toBeDefined()

    await queryButton!.trigger('click')
    await flushPromises()

    expect(getCodeResultMock).toHaveBeenCalledWith(
      1024
    )
    expect(wrapper.text()).toContain('判分完成')
    expect(wrapper.text()).toContain('95')
    expect(wrapper.text()).toContain(
      'Hello EduAgent'
    )
    expect(wrapper.text()).toContain(
      '代码结构清晰'
    )
  })
})