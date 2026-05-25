import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  // 学生端登录/注册
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue') },
  { path: '/register/form', name: 'RegisterForm', component: () => import('@/views/RegisterForm.vue') },
  
  // 管理员登录页（关键：这条路由必须存在，且路径为 /admin/login）
  { path: '/admin/login', name: 'AdminLogin', component: () => import('@/views/admin/AdminLogin.vue') },

  // 学生端布局
  {
    path: '/student',
    name: 'StudentLayout',
    component: () => import('@/layouts/StudentLayout.vue'),
    redirect: '/student/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/student/Dashboard.vue') },
      { path: 'profile/chat', component: () => import('@/views/student/ProfileChat.vue') },
      { path: 'profile/overview', component: () => import('@/views/student/ProfileOverview.vue') },
      { path: 'courses', component: () => import('@/views/student/CourseCenter.vue') },
      { path: 'courses/:id', component: () => import('@/views/student/CourseDetail.vue') },
      { path: 'resources', component: () => import('@/views/student/ResourceCenter.vue') },
      { path: 'resources/:id', component: () => import('@/views/student/ResourceDetail.vue') },
      { path: 'path', component: () => import('@/views/student/LearningPath.vue') },
      { path: 'tutor', component: () => import('@/views/student/TutorChat.vue') },
      { path: 'report', component: () => import('@/views/student/Report.vue') },
      { path: 'tasks', component: () => import('@/views/student/LearningTask.vue') },
      { path: 'practice', component: () => import('@/views/student/Practice.vue') },
      { path: 'projects', component: () => import('@/views/student/Projects.vue') },
      { path: 'messages', component: () => import('@/views/student/Messages.vue') },
      { path: 'settings', component: () => import('@/views/student/Settings.vue') },
      { path: 'profile', component: () => import('@/views/student/Profile.vue') }
    ]
  },

  // 管理端布局
  {
    path: '/admin',
    name: 'AdminLayout',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'users', component: () => import('@/views/admin/UserManage.vue') },
      { path: 'resources', component: () => import('@/views/admin/ResourceManage.vue') },
      { path: 'agents', component: () => import('@/views/admin/AgentManage.vue') },
      { path: 'reviews', component: () => import('@/views/admin/ContentReview.vue') },
      { path: 'statistics', component: () => import('@/views/admin/Statistics.vue') },
      { path: 'settings', component: () => import('@/views/admin/Settings.vue') }
    ]
  },

  // 默认重定向
  { path: '/', redirect: '/student/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// ===================== 路由守卫（关键修复点）=====================
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')

  // 1. **管理端特殊处理**：除了 /admin/login 之外的所有 /admin/* 都需要登录
  if (to.path.startsWith('/admin')) {
    if (to.path === '/admin/login') {
      // 访问管理员登录页，直接放行（不需要 token）
      next()
      return
    } else {
      // 访问其他管理端页面，必须要有 token
      if (!token) {
        next('/admin/login')
      } else {
        next()
      }
      return
    }
  }

  // 2. **学生端处理**：访问 /login 或 /register 放行，其他需 token
  if (to.path === '/login' || to.path === '/register') {
    next()
    return
  }

  if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router
