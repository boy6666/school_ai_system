import {
  afterAll,
  beforeEach,
  describe,
  expect,
  it,
  vi
} from 'vitest'
import AxiosMockAdapter from 'axios-mock-adapter'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn()
  }
}))

import request from '@/utils/request'
import {
  addClassStudent,
  createTeacherClass,
  deleteTeacherClass,
  getClassStudents,
  getTeacherClass,
  getTeacherClasses,
  removeClassStudent,
  updateTeacherClass
} from '@/api/teacher'

const mock = new AxiosMockAdapter(request)
const classPath = '/edu-agent-teacher/classes'

describe('教师端班级接口契约', () => {
  beforeEach(() => {
    mock.reset()
  })

  afterAll(() => {
    mock.restore()
  })

  it('应按正式路径查询班级列表与详情', async () => {
    mock.onGet(classPath).reply(200, {
      code: 0,
      message: 'success',
      data: [
        {
          id: 1,
          name: '计科2301',
          teacherId: 3,
          course: 'Java 程序设计',
          semester: '2026秋',
          status: 1,
          createTime: '2026-08-08T10:00:00',
          studentCount: 0
        }
      ]
    })

    mock.onGet(`${classPath}/1`).reply(200, {
      code: 0,
      message: 'success',
      data: {
        id: 1,
        name: '计科2301',
        teacherId: 3,
        course: 'Java 程序设计',
        semester: '2026秋',
        status: 1,
        createTime: '2026-08-08T10:00:00',
        studentCount: 0
      }
    })

    const list = await getTeacherClasses()
    const detail = await getTeacherClass(1)

    expect(list).toHaveLength(1)
    expect(detail.id).toBe(1)
    expect(mock.history.get[0]?.url).toBe(classPath)
    expect(mock.history.get[1]?.url).toBe(`${classPath}/1`)
  })

  it('应按正式请求体创建和更新班级', async () => {
    const createData = {
      name: '计科2301',
      course: 'Java 程序设计',
      semester: '2026秋'
    }

    const updateData = {
      course: 'Java 程序设计（升级版）'
    }

    mock.onPost(classPath).reply(200, {
      code: 0,
      message: 'success',
      data: {
        id: 1,
        teacherId: 3,
        status: 1,
        createTime: '2026-08-08T10:00:00',
        studentCount: 0,
        ...createData
      }
    })

    mock.onPut(`${classPath}/1`).reply(200, {
      code: 0,
      message: 'success',
      data: {
        id: 1,
        name: '计科2301',
        teacherId: 3,
        semester: '2026秋',
        status: 1,
        createTime: '2026-08-08T10:00:00',
        studentCount: 0,
        ...updateData
      }
    })

    await createTeacherClass(createData)
    await updateTeacherClass(1, updateData)

    expect(JSON.parse(mock.history.post[0]?.data)).toEqual(
      createData
    )
    expect(mock.history.post[0]?.url).toBe(classPath)
    expect(JSON.parse(mock.history.put[0]?.data)).toEqual(
      updateData
    )
    expect(mock.history.put[0]?.url).toBe(`${classPath}/1`)
  })

  it('应按正式路径删除班级', async () => {
    mock.onDelete(`${classPath}/1`).reply(200, {
      code: 0,
      message: 'success',
      data: null
    })

    await deleteTeacherClass(1)

    expect(mock.history.delete[0]?.url).toBe(
      `${classPath}/1`
    )
  })

  it('应按正式契约管理班级学生', async () => {
    const studentsPath = `${classPath}/1/students`

    mock.onGet(studentsPath).reply(200, {
      code: 0,
      message: 'success',
      data: [
        {
          studentId: 12,
          studentName: '张三',
          joinedAt: '2026-08-08T10:05:00'
        }
      ]
    })

    mock.onPost(studentsPath).reply(200, {
      code: 0,
      message: 'success',
      data: null
    })

    mock.onDelete(`${studentsPath}/12`).reply(200, {
      code: 0,
      message: 'success',
      data: null
    })

    const students = await getClassStudents(1)
    await addClassStudent(1, { studentId: 12 })
    await removeClassStudent(1, 12)

    expect(students[0]?.studentId).toBe(12)
    expect(mock.history.get[0]?.url).toBe(studentsPath)
    expect(mock.history.post[0]?.url).toBe(studentsPath)
    expect(JSON.parse(mock.history.post[0]?.data)).toEqual({
      studentId: 12
    })
    expect(mock.history.delete[0]?.url).toBe(
      `${studentsPath}/12`
    )
  })
})