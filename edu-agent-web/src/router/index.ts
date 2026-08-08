import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'


const routes: RouteRecordRaw[] = [

  // 学生端登录/注册
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


  // 管理员登录页
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/AdminLogin.vue')
  },


  // =====================
  // 学生端
  // =====================

  {
    path: '/student',
    name: 'StudentLayout',

    component: () =>
      import('@/layouts/StudentLayout.vue'),

    redirect: '/student/dashboard',

    children: [

      {
        path: 'dashboard',
        component: () =>
          import('@/views/student/Dashboard.vue')
      },

      {
        path: 'profile/settings',
        component: () =>
          import('@/views/student/Profile.vue')
      },

      {
        path: 'profile/overview',
        component: () =>
          import('@/views/student/ProfileOverview.vue')
      },


      {
        path: 'courses',
        component: () =>
          import('@/views/student/CourseCenter.vue')
      },


      {
        path: 'courses/:id',
        component: () =>
          import('@/views/student/CourseDetail.vue')
      },


      {
        path: 'resources',
        component: () =>
          import('@/views/student/ResourceCenter.vue')
      },


      {
        path: 'resources/generate',
        component: () =>
          import('@/views/student/ResourceGenerate.vue')
      },


      {
        path: 'resources/generate/:type',
        component: () =>
          import('@/views/student/ResourceGenerate.vue')
      },


      {
        path: 'resources/:id',
        component: () =>
          import('@/views/student/ResourceDetail.vue')
      },


      {
        path: 'path',
        component: () =>
          import('@/views/student/LearningPath.vue')
      },


      {
        path: 'wrong-questions',
        component: () =>
          import('@/views/student/WrongQuestionList.vue')
      },


      {
        path: 'wrong-questions/:id',
        component: () =>
          import('@/views/student/WrongQuestionDetail.vue')
      },


      {
        path: 'tutor',
        component: () =>
          import('@/views/student/TutorChat.vue')
      },


      {
        path: 'report',
        component: () =>
          import('@/views/student/Report.vue')
      },


      {
        path: 'tasks',
        component: () =>
          import('@/views/student/LearningTask.vue')
      },


      {
        path: 'practice',
        component: () =>
          import('@/views/student/Practice.vue')
      },


      {
        path: 'projects',
        component: () =>
          import('@/views/student/Projects.vue')
      },


      {
        path: 'messages',
        component: () =>
          import('@/views/student/Messages.vue')
      },


      {
        path: 'settings',
        component: () =>
          import('@/views/student/Settings.vue')
      },


      {
        path: 'profile',
        component: () =>
          import('@/views/student/ProfileOverview.vue')
      }

    ]

  },



  // =====================
  // 管理端
  // =====================

  {
    path: '/admin',

    name: 'AdminLayout',

    component: () =>
      import('@/layouts/AdminLayout.vue'),


    redirect: '/admin/dashboard',


    children: [

      {
        path: 'dashboard',
        component: () =>
          import('@/views/admin/Dashboard.vue')
      },


      {
        path: 'users',
        component: () =>
          import('@/views/admin/UserManage.vue')
      },


      {
        path: 'resources',
        component: () =>
          import('@/views/admin/ResourceManage.vue')
      },


      {
        path: 'reviews',
        component: () =>
          import('@/views/admin/ContentReview.vue')
      },


      {
        path: 'statistics',
        component: () =>
          import('@/views/admin/Statistics.vue')
      },


      {
        path: 'settings',
        component: () =>
          import('@/views/admin/Settings.vue')
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




// ===============================
// 路由守卫
// roles权限控制
// ===============================


function getRoles(): string[] {

  try {

    return JSON.parse(
      localStorage.getItem('roles') || '[]'
    )

  } catch {

    return []

  }

}



router.beforeEach((to) => {


  const token =
    localStorage.getItem('token')


  const roles =
    getRoles()



  // 白名单

  if (

    to.path === '/login'

    ||

    to.path === '/register'

    ||

    to.path === '/admin/login'

  ) {

    return true

  }



  // 未登录

  if (!token) {


    if (
      to.path.startsWith('/admin')
    ) {

      return '/admin/login'

    }


    return '/login'

  }




  // 管理端权限

  if (
    to.path.startsWith('/admin')
  ) {


    if (
      !roles.includes('ROLE_ADMIN')
    ) {


      if (
        roles.includes('ROLE_TEACHER')
      ) {

        return '/teacher/dashboard'

      }


      return '/student/dashboard'

    }

  }




  // 学生端权限

  if (
    to.path.startsWith('/student')
  ) {


    if (
      roles.includes('ROLE_ADMIN')
      ||
      roles.includes('ROLE_TEACHER')
    ) {

      if (
        roles.includes('ROLE_ADMIN')
      ) {

        return '/admin/dashboard'

      }


      return '/teacher/dashboard'

    }

  }




  // 教师端预留

  if (
    to.path.startsWith('/teacher')
  ) {


    if (
      !roles.includes('ROLE_TEACHER')
    ) {


      if (
        roles.includes('ROLE_ADMIN')
      ) {

        return '/admin/dashboard'

      }


      return '/student/dashboard'

    }

  }



  return true


})



export default router