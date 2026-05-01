import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/student/resources'
  },
  {
    path: '/student',
    component: () => import('@/layouts/StudentLayout.vue'),
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
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router