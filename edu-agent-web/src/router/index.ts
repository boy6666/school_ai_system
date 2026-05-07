import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/student/dashboard'
  },

  // 学生端
  {
    path: '/student',
    component: () => import('@/layouts/StudentLayout.vue'),
    redirect: '/student/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'StudentDashboard',
        component: () => import('@/views/student/Dashboard.vue'),
        meta: {
          title: '学习首页'
        }
      },
      {
        path: 'practice',
        name: 'StudentPractice',
        component: () => import('@/views/student/Practice.vue'),
        meta: {
          title: '练习/题库'
        }
      },
      {
        path: 'projects',
        name: 'StudentProjects',
        component: () => import('@/views/student/Projects.vue'),
        meta: {
          title: '实操/项目案例'
        }
      },
      {
        path: 'report',
        name: 'StudentReport',
        component: () => import('@/views/student/Report.vue'),
        meta: {
          title: '学习报告'
        }
      },
      {
        path: 'messages',
        name: 'StudentMessages',
        component: () => import('@/views/student/Messages.vue'),
        meta: {
          title: '消息中心'
        }
      },
      {
        path: 'profile',
        name: 'StudentProfile',
        component: () => import('@/views/student/Profile.vue'),
        meta: {
          title: '个人中心'
        }
      },
      {
        path: 'settings',
        name: 'StudentSettings',
        component: () => import('@/views/student/Settings.vue'),
        meta: {
          title: '设置'
        }
      },
      {
        path: 'resources',
        name: 'StudentResourceCenter',
        component: () => import('@/views/student/ResourceCenter.vue'),
        meta: {
          title: '资源中心'
        }
      },
      {
        path: 'resources/:id',
        name: 'StudentResourceDetail',
        component: () => import('@/views/student/ResourceDetail.vue'),
        meta: {
          title: '资源详情'
        }
      },
      {
        path: 'courses',
        name: 'StudentCourseCenter',
        component: () => import('@/views/student/CourseCenter.vue'),
        meta: {
          title: '课程中心'
        }
      },
      {
        path: 'courses/:id',
        name: 'StudentCourseDetail',
        component: () => import('@/views/student/CourseDetail.vue'),
        meta: {
          title: '课堂 / 学习空间'
        }
      },
      {
        path: 'tasks',
        name: 'StudentLearningTask',
        component: () => import('@/views/student/LearningTask.vue'),
        meta: {
          title: '学习任务 / 计划'
        }
      },
      {
        path: 'tutor',
        name: 'StudentTutorChat',
        component: () => import('@/views/student/TutorChat.vue'),
        meta: {
          title: '智能辅导'
        }
      }
    ]
  },

  // 管理端
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/resources',
    children: [
      {
        path: 'resources',
        name: 'AdminResourceManage',
        component: () => import('@/views/admin/ResourceManage.vue'),
        meta: {
          title: '课程 / 资源管理'
        }
      },
      {
        path: 'agents',
        name: 'AdminAgentManage',
        component: () => import('@/views/admin/AgentManage.vue'),
        meta: {
          title: '智能体管理'
        }
      },
      {
        path: 'reviews',
        name: 'AdminContentReview',
        component: () => import('@/views/admin/ContentReview.vue'),
        meta: {
          title: '内容审核'
        }
      }
    ]
  },

  // 兜底
  {
    path: '/:pathMatch(.*)*',
    redirect: '/student/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.afterEach(to => {
  const title = to.meta.title ? `${String(to.meta.title)} - EduAgent` : 'EduAgent'
  document.title = title
})

export default router