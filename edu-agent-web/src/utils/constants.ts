export const ROLE = {
  STUDENT: 'ROLE_STUDENT',
  TEACHER: 'ROLE_TEACHER',
  ADMIN: 'ROLE_ADMIN'
} as const

export type Role = typeof ROLE[keyof typeof ROLE]

export const ROLE_HOME: Record<Role, string> = {
  [ROLE.STUDENT]: '/student/dashboard',
  [ROLE.TEACHER]: '/teacher/dashboard',
  [ROLE.ADMIN]: '/admin/dashboard'
}