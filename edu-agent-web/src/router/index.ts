import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/student/dashboard'
  },
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
          title: '系统设置'
        }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router