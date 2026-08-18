<template>
  <div
    ref="editorContainer"
    class="code-editor"
    :style="{ height }"
  ></div>
</template>

<script setup lang="ts">
import {
  onBeforeUnmount,
  onMounted,
  ref,
  watch
} from 'vue'
import * as monaco from 'monaco-editor'

const props = withDefaults(
  defineProps<{
    modelValue: string
    language?: string
    height?: string
    readonly?: boolean
  }>(),
  {
    language: 'plaintext',
    height: '420px',
    readonly: true
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const workerEnvironment = self as typeof self & {
  MonacoEnvironment?: {
    getWorker: (
      moduleId: string,
      label: string
    ) => Worker
  }
}

workerEnvironment.MonacoEnvironment = {
  getWorker(_moduleId: string, label: string) {
    if (label === 'json') {
      return new Worker(
        new URL(
          '../../node_modules/monaco-editor/esm/vs/language/json/json.worker.js',
          import.meta.url
        ),
        {
          type: 'module'
        }
      )
    }

    if (
      label === 'css' ||
      label === 'scss' ||
      label === 'less'
    ) {
      return new Worker(
        new URL(
          '../../node_modules/monaco-editor/esm/vs/language/css/css.worker.js',
          import.meta.url
        ),
        {
          type: 'module'
        }
      )
    }

    if (
      label === 'html' ||
      label === 'handlebars' ||
      label === 'razor'
    ) {
      return new Worker(
        new URL(
          '../../node_modules/monaco-editor/esm/vs/language/html/html.worker.js',
          import.meta.url
        ),
        {
          type: 'module'
        }
      )
    }

    if (
      label === 'typescript' ||
      label === 'javascript'
    ) {
      return new Worker(
        new URL(
          '../../node_modules/monaco-editor/esm/vs/language/typescript/ts.worker.js',
          import.meta.url
        ),
        {
          type: 'module'
        }
      )
    }

    return new Worker(
      new URL(
        '../../node_modules/monaco-editor/esm/vs/editor/editor.worker.js',
        import.meta.url
      ),
      {
        type: 'module'
      }
    )
  }
}

const editorContainer = ref<HTMLElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null
let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  if (!editorContainer.value) return

  editor = monaco.editor.create(
    editorContainer.value,
    {
      value: props.modelValue,
      language: props.language,
      theme: 'vs-dark',
      readOnly: props.readonly,
      automaticLayout: false,
      minimap: {
        enabled: false
      },
      scrollBeyondLastLine: false,
      fontSize: 14,
      lineHeight: 22,
      wordWrap: 'on',
      tabSize: 2
    }
  )

  editor.onDidChangeModelContent(() => {
    if (!props.readonly) {
      emit(
        'update:modelValue',
        editor?.getValue() || ''
      )
    }
  })

  resizeObserver = new ResizeObserver(() => {
    editor?.layout()
  })

  resizeObserver.observe(editorContainer.value)
})

watch(
  () => props.modelValue,
  value => {
    if (editor && editor.getValue() !== value) {
      editor.setValue(value)
    }
  }
)

watch(
  () => props.language,
  language => {
    const model = editor?.getModel()

    if (model) {
      monaco.editor.setModelLanguage(
        model,
        language
      )
    }
  }
)

watch(
  () => props.readonly,
  readonly => {
    editor?.updateOptions({
      readOnly: readonly
    })
  }
)

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  editor?.dispose()
  editor = null
})
</script>

<style scoped>
.code-editor {
  width: 100%;
  min-height: 240px;
  overflow: hidden;
  border-radius: 0 0 8px 8px;
}
</style>