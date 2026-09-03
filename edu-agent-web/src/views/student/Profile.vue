<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="profile-card">
          <div class="profile-header">
            <div class="avatar-section">
              <el-avatar :size="100" :src="userInfo?.avatar" @click="changeAvatar">
                {{ userInfo?.name?.charAt(0) }}
              </el-avatar>
              <el-button link type="primary" @click="changeAvatar">更换头像</el-button>
            </div>
            <h3>{{ userInfo?.name }}</h3>
            <p class="user-email">{{ userInfo?.email }}</p>
          </div>

          <el-divider />

          <div class="menu-list">
            <div
              class="menu-item"
              :class="{ active: activeMenu === 'info' }"
              @click="activeMenu = 'info'"
            >
              <el-icon><User /></el-icon>
              <span>基本信息</span>
            </div>
            <div
              class="menu-item"
              :class="{ active: activeMenu === 'security' }"
              @click="activeMenu = 'security'"
            >
              <el-icon><Lock /></el-icon>
              <span>账户安全</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="18">
        <el-card class="content-card">
          <!-- 基本信息 -->
          <div v-if="activeMenu === 'info'" class="info-section">
            <h2 class="section-title">基本信息</h2>
            <el-form :model="profileForm" label-width="100px" class="profile-form">
              <el-form-item label="用户名">
                <el-input v-model="profileForm.username" placeholder="请输入用户名" />
              </el-form-item>
              <el-form-item label="真实姓名">
                <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
              </el-form-item>
              <el-form-item label="个人简介">
                <el-input
                  v-model="profileForm.bio"
                  type="textarea"
                  :rows="4"
                  placeholder="介绍一下你自己..."
                />
              </el-form-item>
              <el-form-item label="所在城市">
                <el-cascader
                  v-model="profileForm.city"
                  :options="cityOptions"
                  placeholder="请选择城市"
                />
              </el-form-item>
              <el-form-item label="专业领域">
                <el-select v-model="profileForm.major" placeholder="请选择专业领域">
                  <el-option label="前端开发" value="frontend" />
                  <el-option label="后端开发" value="backend" />
                  <el-option label="全栈开发" value="fullstack" />
                  <el-option label="移动开发" value="mobile" />
                  <el-option label="数据分析" value="data" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveProfile">保存修改</el-button>
                <el-button @click="resetProfile">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 账户安全 -->
          <div v-if="activeMenu === 'security'" class="security-section">
            <h2 class="section-title">账户安全</h2>

            <div class="security-item">
              <div class="item-left">
                <el-icon class="item-icon"><Lock /></el-icon>
                <div class="item-info">
                  <h4>修改密码</h4>
                  <p>定期修改密码可以提高账户安全性</p>
                </div>
              </div>
              <el-button @click="showPasswordDialog">修改</el-button>
            </div>

            <div class="security-item">
              <div class="item-left">
                <el-icon class="item-icon"><Cellphone /></el-icon>
                <div class="item-info">
                  <h4>绑定手机</h4>
                  <p>已绑定：{{ userInfo?.phone || '未绑定' }}</p>
                </div>
              </div>
              <el-button @click="showPhoneDialog">{{ userInfo?.phone ? '更换' : '绑定' }}</el-button>
            </div>

            <div class="security-item">
              <div class="item-left">
                <el-icon class="item-icon"><Message /></el-icon>
                <div class="item-info">
                  <h4>绑定邮箱</h4>
                  <p>已绑定：{{ userInfo?.email }}</p>
                </div>
              </div>
              <el-button @click="showEmailDialog">更换</el-button>
            </div>

            <div class="security-item">
              <div class="item-left">
                <el-icon class="item-icon"><Key /></el-icon>
                <div class="item-info">
                  <h4>两步验证</h4>
                  <p>启用两步验证可进一步提高账户安全</p>
                </div>
              </div>
              <el-switch v-model="twoFactorAuth" @change="handleTwoFactorChange" />
            </div>

            <el-divider />

            <div class="danger-zone">
              <h3>危险区域</h3>
              <p>以下操作不可逆，请谨慎操作</p>
              <el-button type="danger" @click="showDeleteDialog">注销账户</el-button>
            </div>
          </div>

          <!-- 学习偏好已移除 -->

<!-- 消息通知已移除 -->
</el-card>
      </el-col>
    </el-row>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px">
      <el-form :model="passwordForm" label-width="80px">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请确认新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="changePassword">确定</el-button>
      </template>
    </el-dialog>

    <!-- 注销账户对话框 -->
    <el-dialog v-model="deleteDialogVisible" title="注销账户" width="400px">
      <el-alert
        title="警告"
        type="warning"
        description="注销账户后，您的所有数据将被永久删除，此操作不可恢复！"
        :closable="false"
        show-icon
      />
      <el-form :model="deleteForm" label-width="80px" style="margin-top: 20px">
        <el-form-item label="密码">
          <el-input v-model="deleteForm.password" type="password" placeholder="请输入密码确认" />
        </el-form-item>
        <el-form-item label="确认">
          <el-checkbox v-model="deleteForm.confirmed">我确认要注销账户</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="deleteAccount" :disabled="!deleteForm.confirmed">确定注销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  User, Lock, Cellphone, Message, Key
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const activeMenu = ref('info')
const twoFactorAuth = ref(false)
const passwordDialogVisible = ref(false)
const deleteDialogVisible = ref(false)

const profileForm = reactive({
  username: userInfo.value?.name || '',
  realName: '',
  email: userInfo.value?.email || '',
  phone: '',
  bio: '',
  city: [],
  major: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})



const deleteForm = reactive({
  password: '',
  confirmed: false
})

const cityOptions = [
  {
    value: 'beijing',
    label: '北京',
    children: [
      { value: 'chaoyang', label: '朝阳区' },
      { value: 'haidian', label: '海淀区' }
    ]
  },
  {
    value: 'shanghai',
    label: '上海',
    children: [
      { value: 'pudong', label: '浦东新区' },
      { value: 'minhang', label: '闵行区' }
    ]
  }
]

const changeAvatar = () => {
  ElMessage.info('头像上传功能开发中...')
}

const saveProfile = () => {
  ElMessage.info('基本信息修改接口暂未开放')
}

const resetProfile = () => {
  profileForm.username = userInfo.value?.name || ''
  profileForm.email = userInfo.value?.email || ''
  profileForm.realName = ''
  profileForm.phone = ''
  profileForm.bio = ''
  profileForm.city = []
  profileForm.major = ''
  ElMessage.info('已重置为原始信息')
}

const showPasswordDialog = () => {
  passwordDialogVisible.value = true
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const changePassword = () => {
  ElMessage.info('修改密码接口暂未开放')
}

const showPhoneDialog = () => {
  ElMessage.info('手机绑定功能开发中...')
}

const showEmailDialog = () => {
  ElMessage.info('邮箱修改功能开发中...')
}

const handleTwoFactorChange = (value: boolean) => {
  twoFactorAuth.value = !value
  ElMessage.info('两步验证接口暂未开放')
}

const showDeleteDialog = () => {
  deleteDialogVisible.value = true
  deleteForm.password = ''
  deleteForm.confirmed = false
}

const deleteAccount = () => {
  ElMessage.info('账户注销接口暂未开放')
}



</script>

<style scoped>
.profile-page {
  padding: 20px;
}

.profile-card {
  height: 100%;
  border-radius: 8px;
}

.profile-header {
  text-align: center;
  padding: 20px 0;
}

.avatar-section {
  margin-bottom: 15px;
}

.profile-header h3 {
  margin: 15px 0 5px 0;
  font-size: 20px;
  color: #333;
}

.user-email {
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
}

.user-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
}

.menu-list {
  padding: 10px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px 20px;
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 6px;
  color: #666;
}

.menu-item:hover {
  background: #f5f7fa;
}

.menu-item.active {
  background: #409eff;
  color: #fff;
}

.menu-item .el-icon {
  font-size: 18px;
}

.content-card {
  border-radius: 8px;
  min-height: 600px;
}

.section-title {
  margin: 0 0 30px 0;
  font-size: 20px;
  color: #333;
  padding-bottom: 15px;
  border-bottom: 2px solid #409eff;
}

.profile-form {
  max-width: 600px;
}

.unit {
  margin-left: 10px;
  color: #666;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 15px;
}

.item-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.item-icon {
  font-size: 28px;
  color: #409eff;
}

.item-info h4 {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: #333;
}

.item-info p {
  margin: 0;
  font-size: 14px;
  color: #999;
}

.danger-zone {
  padding: 20px;
  background: #fef0f0;
  border-radius: 8px;
  border: 1px solid #fde2e2;
}

.danger-zone h3 {
  margin: 0 0 10px 0;
  color: #f56c6c;
}

.danger-zone p {
  margin: 0 0 15px 0;
  color: #999;
}


</style>