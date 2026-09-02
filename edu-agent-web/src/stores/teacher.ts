import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getTeacherClasses } from '@/api/teacher'
import type {
  CreateAssignmentRequest,
  TeacherClass
} from '@/api/teacher'

export const useTeacherStore = defineStore(
  'teacher',
  () => {
    const classes = ref<TeacherClass[]>([])
    const selectedClassId = ref<number>()
    const assignmentDraft =
      ref<CreateAssignmentRequest | null>(null)
    const loadingClasses = ref(false)

    const selectedClass = computed(() =>
      classes.value.find(
        item => item.id === selectedClassId.value
      )
    )

    async function loadClasses(): Promise<TeacherClass[]> {
      loadingClasses.value = true

      try {
        const result = await getTeacherClasses()
        classes.value = result

        const selectionExists = result.some(
          item => item.id === selectedClassId.value
        )

        if (!selectionExists) {
          selectedClassId.value = result[0]?.id
        }

        return result
      } finally {
        loadingClasses.value = false
      }
    }

    function selectClass(classId?: number): void {
      selectedClassId.value = classId
    }

    function setAssignmentDraft(
      draft: CreateAssignmentRequest
    ): void {
      assignmentDraft.value = draft
    }

    function clearAssignmentDraft(): void {
      assignmentDraft.value = null
    }

    function reset(): void {
      classes.value = []
      selectedClassId.value = undefined
      assignmentDraft.value = null
      loadingClasses.value = false
    }

    return {
      classes,
      selectedClassId,
      selectedClass,
      assignmentDraft,
      loadingClasses,
      loadClasses,
      selectClass,
      setAssignmentDraft,
      clearAssignmentDraft,
      reset
    }
  }
)