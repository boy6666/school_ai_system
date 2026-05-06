import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/StudentLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/student/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'practice',
        name: 'Practice',
        component: () => import('@/views/student/Practice.vue'),
        meta: { title: '练习/题库' }
      },
      {
        path: 'projects',
        name: 'Projects',
        component: () => import('@/views/student/Projects.vue'),
        meta: { title: '实操/项目案例' }
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/student/Report.vue'),
        meta: { title: '学习报告' }
      },
      {
        path: 'messages',
        name: 'Messages',
        component: () => import('@/views/student/Messages.vue'),
        meta: { title: '消息中心' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/student/Profile.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/student/Settings.vue'),
        meta: { title: '设置' }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '管理首页' }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/admin/Statistics.vue'),
        meta: { title: '数据统计与分析' }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/Settings.vue'),
        meta: { title: '系统设置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || 'EduAgent'} - 学习平台`

  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
