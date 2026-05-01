import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/student/resources'
  },

  // 学生端
  {
    path: '/student',
    component: () => import('@/layouts/StudentLayout.vue'),
    redirect: '/student/resources',
    children: [
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
        path: 'courses/:id',
        name: 'StudentCourseDetail',
        component: () => import('@/views/student/CourseDetail.vue'),
        meta: {
          title: '课堂 / 学习空间'
        }
      }
    ]
  },

  // 管理端
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: {
          title: '管理后台首页'
        }
      },
      {
        path: 'users',
        name: 'AdminUserManage',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: {
          title: '用户管理'
        }
      },
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
      },
      {
        path: 'statistics',
        name: 'AdminStatistics',
        component: () => import('@/views/admin/Statistics.vue'),
        meta: {
          title: '数据统计'
        }
      },
      {
        path: 'settings',
        name: 'AdminSystemSetting',
        component: () => import('@/views/admin/SystemSetting.vue'),
        meta: {
          title: '系统设置'
        }
      }
    ]
  },

  // 兜底
  {
    path: '/:pathMatch(.*)*',
    redirect: '/student/resources'
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