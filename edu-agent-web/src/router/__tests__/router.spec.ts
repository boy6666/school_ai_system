import { beforeEach, describe, expect, it } from 'vitest'
import type { RouteLocationNormalized } from 'vue-router'
import router, { authGuard } from '@/router'
import { ROLE } from '@/utils/constants'

function createRoute(
  path: string,
  matched = true
): RouteLocationNormalized {
  return {
    path,
    fullPath: path,
    matched: matched ? [{}] : []
  } as unknown as RouteLocationNormalized
}

function setLogin(roles: string[]) {
  localStorage.setItem('token', 'test-token')
  localStorage.setItem('roles', JSON.stringify(roles))
}

describe('Router authentication and role control', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('redirects unauthenticated student route to login', () => {
    const result = authGuard(
      createRoute('/student/dashboard')
    )

    expect(result).toBe('/login')
  })

  it('redirects unauthenticated admin route to admin login', () => {
    const result = authGuard(
      createRoute('/admin/dashboard')
    )

    expect(result).toBe('/admin/login')
  })

  it('redirects student away from admin route', () => {
    setLogin([ROLE.STUDENT])

    const result = authGuard(
      createRoute('/admin/dashboard')
    )

    expect(result).toBe('/student/dashboard')
  })

  it('redirects admin away from student route', () => {
    setLogin([ROLE.ADMIN])

    const result = authGuard(
      createRoute('/student/dashboard')
    )

    expect(result).toBe('/admin/dashboard')
  })

  it('clears invalid role state and redirects to login', () => {
    setLogin(['INVALID_ROLE'])

    const result = authGuard(
      createRoute('/student/dashboard')
    )

    expect(result).toBe('/login')
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('roles')).toBeNull()
  })

  it('allows student to access student route', () => {
    setLogin([ROLE.STUDENT])

    const result = authGuard(
      createRoute('/student/dashboard')
    )

    expect(result).toBe(true)
  })
  it('allows teacher to access teacher route', () => {
    setLogin([ROLE.TEACHER])

    const result = authGuard(
      createRoute('/teacher/dashboard')
    )

    expect(result).toBe(true)
  })

  it('redirects student away from teacher route', () => {
    setLogin([ROLE.STUDENT])

    const result = authGuard(
      createRoute('/teacher/dashboard')
    )

    expect(result).toBe('/student/dashboard')
  })

  it('redirects teacher away from student and admin routes', () => {
    setLogin([ROLE.TEACHER])

    const studentResult = authGuard(
      createRoute('/student/dashboard')
    )
    const adminResult = authGuard(
      createRoute('/admin/dashboard')
    )

    expect(studentResult).toBe('/teacher/dashboard')
    expect(adminResult).toBe('/teacher/dashboard')
  })
    it('registers all teacher routes', () => {
    const routeNames = router
      .getRoutes()
      .map(route => route.name)

    expect(routeNames).toEqual(
      expect.arrayContaining([
        'TeacherDashboard',
        'TeacherClasses',
        'TeacherQuestions',
        'TeacherAssignments',
        'TeacherGrades',
        'TeacherAnalytics',
        'TeacherAiTutor',
        'TeacherResources'
      ])
    )
  })
})