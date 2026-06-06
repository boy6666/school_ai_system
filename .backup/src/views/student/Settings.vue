<template>
  <div class="settings-page">
    <el-card class="settings-card">
      <template #header>
        <h2>设置</h2>
      </template>

      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="账户设置" name="account">
          <div class="settings-content">
            <el-form :model="settingsForm" label-width="120px">
              <el-form-item label="显示名称">
                <el-input v-model="settingsForm.displayName" placeholder="设置显示名称" />
              </el-form-item>
              <el-form-item label="个人简介">
                <el-input
                  v-model="settingsForm.bio"
                  type="textarea"
                  :rows="3"
                  placeholder="写一段个人简介..."
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveSettings">保存设置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="界面设置" name="interface">
          <div class="settings-content">
            <el-form label-width="120px">
              <el-form-item label="语言设置">
                <el-select v-model="settingsForm.language">
                  <el-option label="简体中文" value="zh-CN" />
                  <el-option label="English" value="en-US" />
                </el-select>
              </el-form-item>
              <el-form-item label="界面主题">
                <el-radio-group v-model="settingsForm.theme">
                  <el-radio label="light">浅色</el-radio>
                  <el-radio label="dark">深色</el-radio>
                  <el-radio label="auto">跟随系统</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="字体大小">
                <el-radio-group v-model="settingsForm.fontSize">
                  <el-radio label="small">小</el-radio>
                  <el-radio label="medium">中</el-radio>
                  <el-radio label="large">大</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveSettings">保存设置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="隐私设置" name="privacy">
          <div class="settings-content">
            <el-form label-width="120px">
              <el-form-item label="个人资料可见性">
                <el-select v-model="settingsForm.profileVisibility">
                  <el-option label="所有人可见" value="public" />
                  <el-option label="仅好友可见" value="friends" />
                  <el-option label="仅自己可见" value="private" />
                </el-select>
              </el-form-item>
              <el-form-item label="在线状态">
                <el-switch v-model="settingsForm.showOnlineStatus" />
              </el-form-item>
              <el-form-item label="学习进度可见性">
                <el-switch v-model="settingsForm.showProgress" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveSettings">保存设置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="关于" name="about">
          <div class="about-content">
            <div class="about-item">
              <h3>EduAgent 智能学习平台</h3>
              <p>版本号：1.0.0</p>
              <p>基于 Vue 3 + Spring Boot + AI 技术构建的智能学习平台</p>
            </div>
            <el-divider />
            <div class="about-item">
              <h4>联系我们</h4>
              <p>邮箱：support@eduagent.com</p>
              <p>电话：400-xxx-xxxx</p>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('account')

const settingsForm = reactive({
  displayName: '',
  bio: '',
  language: 'zh-CN',
  theme: 'light',
  fontSize: 'medium',
  profileVisibility: 'public',
  showOnlineStatus: true,
  showProgress: true
})

const saveSettings = () => {
  ElMessage.success('设置保存成功！')
}
</script>

<style scoped>
.settings-page {
  padding: 16px;
}

.settings-card {
  border-radius: 8px;
  max-width: 800px;
  margin: 0 auto;
}

.settings-card h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.settings-content {
  padding: 16px 0;
}

.about-content {
  padding: 16px 0;
}

.about-item {
  margin-bottom: 32px;
}

.about-item h3 {
  margin: 0 0 16px 0;
  font-size: 20px;
  color: #333;
}

.about-item h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #333;
}

.about-item p {
  margin: 4px 0;
  color: #666;
  line-height: 1.6;
}
</style>
