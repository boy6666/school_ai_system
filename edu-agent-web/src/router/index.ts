import { createRouter, createWebHistory } from 'vue-router'
import type {
  RouteLocationNormalized,
  RouteRecordRaw
} from 'vue-router'
import { ROLE, ROLE_HOME, type Role } from '@/utils/constants'

const routes: RouteRecordRaw[] = [
  // 学生端登录、注册
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  },

  // 管理员登录
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/AdminLogin.vue')
  },

  // 学生端
  {
    path: '/student',
    name: 'StudentLayout',
    component: () => import('@/layouts/StudentLayout.vue'),
    redirect: '/student/dashboard',
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/student/Dashboard.vue')
      },
      {
        path: 'profile/settings',
        component: () => import('@/views/student/Profile.vue')
      },
      {
        path: 'profile/overview',
        component: () => import('@/views/student/ProfileOverview.vue')
      },
      {
        path: 'courses',
        component: () => import('@/views/student/CourseCenter.vue')
      },
      {
        path: 'courses/:id',
        component: () => import('@/views/student/CourseDetail.vue')
      },
      {
        path: 'resources',
        component: () => import('@/views/student/ResourceCenter.vue')
      },
      {
        path: 'resources/generate',
        component: () => import('@/views/student/ResourceGenerate.vue')
      },
      {
        path: 'resources/generate/:type',
        component: () => import('@/views/student/ResourceGenerate.vue')
      },
      {
        path: 'resources/:id',
        component: () => import('@/views/student/ResourceDetail.vue')
      },
      {
        path: 'path',
        component: () => import('@/views/student/LearningPath.vue')
      },
      {
        path: 'wrong-questions',
        component: () => import('@/views/student/WrongQuestionList.vue')
      },
      {
        path: 'wrong-questions/:id',
        component: () => import('@/views/student/WrongQuestionDetail.vue')
      },
      {
        path: 'tutor',
        component: () => import('@/views/student/TutorChat.vue')
      },
      {
        path: 'report',
        component: () => import('@/views/student/Report.vue')
      },
      {
        path: 'tasks',
        component: () => import('@/views/student/LearningTask.vue')
      },
      {
        path: 'practice',
        component: () => import('@/views/student/Practice.vue')
      },
      {
        path: 'projects',
        component: () => import('@/views/student/Projects.vue')
      },
      {
        path: 'messages',
        component: () => import('@/views/student/Messages.vue')
      },
      {
        path: 'settings',
        component: () => import('@/views/student/Settings.vue')
      },
      {
        path: 'profile',
        component: () => import('@/views/student/ProfileOverview.vue')
      }
    ]
  },
  // 教师端
  {
    path: '/teacher',
    name: 'TeacherLayout',
    component: () => import('@/layouts/TeacherLayout.vue'),
    redirect: '/teacher/dashboard',
     children: [
      {
        path: 'dashboard',
        name: 'TeacherDashboard',
        component: () => import('@/views/teacher/Dashboard.vue')
      },
          {
        path: 'classes',
        name: 'TeacherClasses',
        component: () => import('@/views/teacher/ClassManage.vue')
      },
      {
        path: 'questions',
        name: 'TeacherQuestions',
        component: () => import('@/views/teacher/QuestionBank.vue')
      },
          {
        path: 'assignments',
        name: 'TeacherAssignments',
        component: () =>
          import('@/views/teacher/AssignmentManage.vue')
      },
            {
        path: 'grades',
        name: 'TeacherGrades',
        component: () =>
          import('@/views/teacher/GradeReview.vue')
      },
       {
        path: 'analytics',
        name: 'TeacherAnalytics',
        component: () =>
          import('@/views/teacher/Analytics.vue')
      },
           {
        path: 'ai-tutor',
        name: 'TeacherAiTutor',
        component: () =>
          import('@/views/teacher/AiTutor.vue')
      },
      {
        path: 'resources',
        name: 'TeacherResources',
        component: () =>
          import('@/views/teacher/ResourcePublish.vue')
      }
    ]
  },

  // 管理端
  {
    path: '/admin',
    name: 'AdminLayout',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/admin/Dashboard.vue')
      },
      {
        path: 'users',
        component: () => import('@/views/admin/UserManage.vue')
      },
      {
        path: 'resources',
        component: () => import('@/views/admin/ResourceManage.vue')
      },
      {
        path: 'reviews',
        component: () => import('@/views/admin/ContentReview.vue')
      },
      {
        path: 'statistics',
        component: () => import('@/views/admin/Statistics.vue')
      },
      {
        path: 'settings',
        component: () => import('@/views/admin/Settings.vue')
      }
    ]
  },

  // 默认入口
  {
    path: '/',
    redirect: '/student/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const PUBLIC_PATHS = new Set([
  '/login',
  '/register',
  '/admin/login'
])

function readRoles(): Role[] {
  try {
    const value: unknown = JSON.parse(
      localStorage.getItem('roles') || '[]'
    )

    if (!Array.isArray(value)) {
      return []
    }

    const validRoles = Object.values(ROLE) as Role[]

    return value.filter(
      (role): role is Role =>
        typeof role === 'string' &&
        validRoles.includes(role as Role)
    )
  } catch {
    return []
  }
}

function resolveHome(roles: Role[]): string | null {
  if (roles.includes(ROLE.ADMIN)) {
    return ROLE_HOME[ROLE.ADMIN]
  }

  if (roles.includes(ROLE.TEACHER)) {
    return ROLE_HOME[ROLE.TEACHER]
  }

  if (roles.includes(ROLE.STUDENT)) {
    return ROLE_HOME[ROLE.STUDENT]
  }

  return null
}

function clearInvalidLoginState() {
  localStorage.removeItem('token')
  localStorage.removeItem('roles')
  localStorage.removeItem('role')
  localStorage.removeItem('userInfo')
}

export function authGuard(to: RouteLocationNormalized) {
  // 登录和注册页面无需Token
  if (PUBLIC_PATHS.has(to.path)) {
    return true
  }

  const token = localStorage.getItem('token')

  // 未登录访问管理端时进入管理员登录页
  if (!token) {
    return to.path.startsWith('/admin')
      ? '/admin/login'
      : '/login'
  }

  const roles = readRoles()
  const home = resolveHome(roles)

  // Token存在但没有合法角色，视为无效登录状态
  if (!home) {
    clearInvalidLoginState()

    return to.path.startsWith('/admin')
      ? '/admin/login'
      : '/login'
  }

  // 管理端只允许管理员访问
  if (
    to.path.startsWith('/admin') &&
    !roles.includes(ROLE.ADMIN)
  ) {
    return home
  }

  // 教师端只允许教师访问
  if (
    to.path.startsWith('/teacher') &&
    !roles.includes(ROLE.TEACHER)
  ) {
    return home
  }

  // 管理员和教师不能进入学生端
  if (
    to.path.startsWith('/student') &&
    (
      roles.includes(ROLE.ADMIN) ||
      roles.includes(ROLE.TEACHER)
    )
  ) {
    return home
  }

  // 已登录用户访问未知地址时返回对应首页
  if (to.matched.length === 0) {
    // 避免对应角色首页未匹配时产生重复重定向
    if (to.path === home) {
      return true
    }

    return home
  }
  return true
}

router.beforeEach(authGuard)

export default router