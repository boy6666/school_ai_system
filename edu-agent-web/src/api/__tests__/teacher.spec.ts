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
  updateTeacherClass,
  createTeacherQuestion,
  deleteTeacherQuestion,
  generateTeacherQuestions,
  getTeacherQuestion,
  getTeacherQuestions,
  updateTeacherQuestion,
  addTeacherAssignmentItem,
  createTeacherAssignment,
  deleteTeacherAssignment,
  getTeacherAssignment,
  getTeacherAssignments,
  publishTeacherAssignment,
  updateTeacherAssignment,
  getAssignmentGrades,
  getTeacherGrade,
  updateTeacherGrade
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
  it('应使用正式查询参数筛选题目列表', async () => {
    const questionPath = '/edu-agent-teacher/questions'
    const params = {
      chapter: '第2章 面向对象',
      topic: '继承',
      type: 'choice' as const,
      difficulty: 'easy' as const
    }

    mock.onGet(questionPath).reply(200, {
      code: 0,
      message: 'success',
      data: []
    })

    await getTeacherQuestions(params)

    expect(mock.history.get[0]?.url).toBe(questionPath)
    expect(mock.history.get[0]?.params).toEqual(params)
  })

  it('应按正式路径查询题目详情', async () => {
    const questionPath = '/edu-agent-teacher/questions'

    mock.onGet(`${questionPath}/101`).reply(200, {
      code: 0,
      message: 'success',
      data: {
        id: 101,
        type: 'choice',
        chapter: '第2章 面向对象',
        topic: '继承',
        content: '以下哪个关键字用于继承？',
        options: ['extends', 'implements'],
        answer: 'extends',
        explanation: 'Java使用extends继承',
        difficulty: 'easy',
        creatorId: 3,
        createTime: '2026-08-08T10:00:00'
      }
    })

    const detail = await getTeacherQuestion(101)

    expect(detail.id).toBe(101)
    expect(mock.history.get[0]?.url).toBe(
      `${questionPath}/101`
    )
  })

  it('应使用完整正式请求体新增和更新题目', async () => {
    const questionPath = '/edu-agent-teacher/questions'
    const data = {
      type: 'choice' as const,
      chapter: '第2章 面向对象',
      topic: '继承',
      content: '以下哪个关键字用于继承？',
      options: ['extends', 'implements'],
      answer: 'extends',
      explanation: 'Java使用extends继承',
      difficulty: 'easy' as const
    }

    mock.onPost(questionPath).reply(200, {
      code: 0,
      message: 'success',
      data: { id: 101, creatorId: 3, ...data }
    })

    mock.onPut(`${questionPath}/101`).reply(200, {
      code: 0,
      message: 'success',
      data: { id: 101, creatorId: 3, ...data }
    })

    await createTeacherQuestion(data)
    await updateTeacherQuestion(101, data)

    expect(JSON.parse(mock.history.post[0]?.data)).toEqual(
      data
    )
    expect(mock.history.post[0]?.url).toBe(questionPath)
    expect(JSON.parse(mock.history.put[0]?.data)).toEqual(
      data
    )
    expect(mock.history.put[0]?.url).toBe(
      `${questionPath}/101`
    )
  })

  it('应按正式契约生成草稿并删除题目', async () => {
    const questionPath = '/edu-agent-teacher/questions'
    const generateData = {
      chapter: '第3章 集合',
      topic: 'HashMap',
      type: 'choice' as const,
      difficulty: 'medium' as const,
      count: 5
    }

    mock.onPost(`${questionPath}/generate`).reply(200, {
      code: 0,
      message: 'success',
      data: []
    })

    mock.onDelete(`${questionPath}/101`).reply(200, {
      code: 0,
      message: 'success',
      data: null
    })

    await generateTeacherQuestions(generateData)
    await deleteTeacherQuestion(101)

    expect(mock.history.post[0]?.url).toBe(
      `${questionPath}/generate`
    )
    expect(JSON.parse(mock.history.post[0]?.data)).toEqual(
      generateData
    )
    expect(mock.history.delete[0]?.url).toBe(
      `${questionPath}/101`
    )
  })
  it('应按正式路径查询作业列表与详情', async () => {
    const assignmentPath =
      '/edu-agent-teacher/assignments'

    mock.onGet(assignmentPath).reply(200, {
      code: 0,
      message: 'success',
      data: []
    })

    mock.onGet(`${assignmentPath}/201`).reply(200, {
      code: 0,
      message: 'success',
      data: {
        id: 201,
        classId: 1,
        title: '第一章作业',
        type: 'homework',
        description: '',
        deadline: null,
        status: 0,
        createTime: '2026-08-28T10:00:00',
        items: []
      }
    })

    await getTeacherAssignments(1)
    await getTeacherAssignment(201)

    expect(mock.history.get[0]?.url).toBe(
      assignmentPath
    )
    expect(mock.history.get[0]?.params).toEqual({
      classId: 1
    })
    expect(mock.history.get[1]?.url).toBe(
      `${assignmentPath}/201`
    )
  })

  it('应使用正式请求体创建和更新作业', async () => {
    const assignmentPath =
      '/edu-agent-teacher/assignments'

    const createData = {
      classId: 1,
      title: '集合练习',
      type: 'homework' as const,
      deadline: '2026-09-01T20:00:00',
      description: '完成集合章节练习',
      items: [
        {
          questionId: 101,
          score: 10
        }
      ]
    }

    const updateData = {
      title: '集合练习（修订）',
      deadline: '2026-09-02T20:00:00',
      status: '1' as const
    }

    mock.onPost(assignmentPath).reply(200, {
      code: 0,
      message: 'success',
      data: {
        id: 201,
        ...createData,
        status: 0,
        createTime: '2026-08-28T10:00:00',
        itemCount: 1,
        totalScore: 10
      }
    })

    mock.onPut(`${assignmentPath}/201`).reply(200, {
      code: 0,
      message: 'success',
      data: {
        id: 201,
        classId: 1,
        title: updateData.title,
        type: 'homework',
        deadline: updateData.deadline,
        status: 1,
        createTime: '2026-08-28T10:00:00',
        itemCount: 1,
        totalScore: 10
      }
    })

    await createTeacherAssignment(createData)
    await updateTeacherAssignment(201, updateData)

    expect(mock.history.post[0]?.url).toBe(
      assignmentPath
    )
    expect(JSON.parse(mock.history.post[0]?.data)).toEqual(
      createData
    )
    expect(mock.history.put[0]?.url).toBe(
      `${assignmentPath}/201`
    )
    expect(JSON.parse(mock.history.put[0]?.data)).toEqual(
      updateData
    )
  })

  it('应按正式契约添加题目并发布作业', async () => {
    const assignmentPath =
      '/edu-agent-teacher/assignments'
    const itemData = {
      questionId: 102,
      score: 20
    }

    mock
      .onPost(`${assignmentPath}/201/items`)
      .reply(200, {
        code: 0,
        message: 'success',
        data: null
      })

    mock
      .onPost(`${assignmentPath}/201/publish`)
      .reply(200, {
        code: 0,
        message: 'success',
        data: null
      })

    await addTeacherAssignmentItem(201, itemData)
    await publishTeacherAssignment(201)

    expect(mock.history.post[0]?.url).toBe(
      `${assignmentPath}/201/items`
    )
    expect(JSON.parse(mock.history.post[0]?.data)).toEqual(
      itemData
    )
    expect(mock.history.post[1]?.url).toBe(
      `${assignmentPath}/201/publish`
    )
  })

  it('应按正式路径删除作业', async () => {
    const assignmentPath =
      '/edu-agent-teacher/assignments'

    mock.onDelete(`${assignmentPath}/201`).reply(200, {
      code: 0,
      message: 'success',
      data: null
    })

    await deleteTeacherAssignment(201)

    expect(mock.history.delete[0]?.url).toBe(
      `${assignmentPath}/201`
    )
  })
  it('应按正式路径查询作业成绩并传递学生筛选参数', async () => {
    const gradeListPath =
      '/edu-agent-teacher/assignments/201/grades'

    mock.onGet(gradeListPath).reply(200, {
      code: 0,
      message: 'success',
      data: []
    })

    await getAssignmentGrades(201, 12)

    expect(mock.history.get[0]?.url).toBe(
      gradeListPath
    )
    expect(mock.history.get[0]?.params).toEqual({
      studentId: 12
    })
  })

  it('应按正式路径查询成绩详情', async () => {
    const gradePath = '/edu-agent-teacher/grades/301'

    mock.onGet(gradePath).reply(200, {
      code: 0,
      message: 'success',
      data: {
        id: 301,
        assignmentId: 201,
        studentId: 12,
        itemId: 401,
        type: 'code',
        language: 'java',
        submission: 'class Main {}',
        score: 80,
        status: 1,
        gradedAt: '2026-08-28T10:00:00',
        hasAiReport: true,
        runResult: '{}',
        staticReport: '{}',
        aiReport: '{}',
        comment: ''
      }
    })

    await getTeacherGrade(301)

    expect(mock.history.get[0]?.url).toBe(
      gradePath
    )
  })

  it('应使用正式请求体复核成绩', async () => {
    const gradePath = '/edu-agent-teacher/grades/301'
    const updateData = {
      score: 85,
      comment: '思路正确，注意边界处理',
      aiReportOverride: '复核后的建议'
    }

    mock.onPut(gradePath).reply(200, {
      code: 0,
      message: 'success',
      data: {
        id: 301,
        assignmentId: 201,
        studentId: 12,
        itemId: 401,
        type: 'code',
        language: 'java',
        submission: 'class Main {}',
        score: 85,
        status: 1,
        gradedAt: '2026-08-28T10:00:00',
        hasAiReport: true,
        runResult: '{}',
        staticReport: '{}',
        aiReport: '{}',
        comment: updateData.comment
      }
    })

    await updateTeacherGrade(301, updateData)

    expect(mock.history.put[0]?.url).toBe(
      gradePath
    )
    expect(JSON.parse(mock.history.put[0]?.data)).toEqual(
      updateData
    )
  })
})