// API 统一入口
export * from './auth'
export * from './user'
export * from './practice'
export * from './project'
export * from './report'
export * from './message'
export {
  approveReview,
  createBackup,
  createRole,
  createUser,
  deleteBackup,
  deleteRole,
  deleteUser,
  downloadBackup,
  exportData,
  getAdminAgentList,
  getAdminCourseList,
  getAdminResourceList,
  getAdminReviewList,
  getBackupList,
  getLearningData,
  getOperationLogs,
  getRoleList,
  getSystemSettings,
  getUserGrowth,
  getUserList,
  rejectReview,
  restoreBackup,
  saveAgentConfig,
  toggleUserStatus,
  updateAgentStatus,
  updateCourseStatus,
  updateResourceStatus,
  updateRole,
  updateSystemSettings,
  updateUser
} from './admin'
export type {
  AdminAgentListQuery,
  AdminAgentListResponse,
  AdminCourseItem,
  AdminCourseListResponse,
  AdminManageListQuery,
  AdminResourceItem,
  AdminResourceListResponse,
  AdminReviewListQuery,
  AdminReviewListResponse,
  AgentItem,
  AgentStatus,
  ManageStatus,
  ReviewItem,
  ReviewStatus,
  RiskLevel
} from './admin'
