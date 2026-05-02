import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue') },
  {
    path: '/student',
    name: 'StudentLayout',
    component: () => import('@/layouts/StudentLayout.vue'),
    redirect: '/student/dashboard',
    children: [
      { path: 'dashboard', name: 'StudentDashboard', component: () => import('@/views/student/Dashboard.vue') },
      { path: 'profile/chat', name: 'ProfileChat', component: () => import('@/views/student/ProfileChat.vue') },
      { path: 'profile/overview', name: 'ProfileOverview', component: () => import('@/views/student/ProfileOverview.vue') },
      { path: 'resources/generate', name: 'ResourceGenerate', component: () => import('@/views/student/ResourceGenerate.vue') },
      { path: 'resources', name: 'ResourceCenter', component: () => import('@/views/student/ResourceCenter.vue') },
      { path: 'path', name: 'LearningPath', component: () => import('@/views/student/LearningPath.vue') },
      { path: 'tutor', name: 'TutorChat', component: () => import('@/views/student/TutorChat.vue') },
      { path: 'report', name: 'LearningReport', component: () => import('@/views/student/LearningReport.vue') },
      { path: 'profile', name: 'StudentProfile', component: () => import('@/views/student/Profile.vue') }
    ]
  },
  {
    path: '/admin',
    name: 'AdminLayout',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'users', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue') },
      { path: 'resources', name: 'ResourceManage', component: () => import('@/views/admin/ResourceManage.vue') },
      { path: 'agents', name: 'AgentManage', component: () => import('@/views/admin/AgentManage.vue') },
      { path: 'reviews', name: 'ContentReview', component: () => import('@/views/admin/ContentReview.vue') },
      { path: 'statistics', name: 'Statistics', component: () => import('@/views/admin/Statistics.vue') },
      { path: 'settings', name: 'SystemSetting', component: () => import('@/views/admin/SystemSetting.vue') }
    ]
  },
  { path: '/', redirect: '/student/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && to.path !== '/register' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
