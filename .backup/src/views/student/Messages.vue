<template>
  <div class="messages-page">
    <el-card class="messages-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h2>消息中心</h2>
            <el-badge :value="unreadCount" class="unread-badge">
              <span>{{ unreadCount }} 条未读消息</span>
            </el-badge>
          </div>
          <div class="header-right">
            <el-button @click="markAllRead">全部标为已读</el-button>
            <el-button type="danger" @click="clearMessages">清空消息</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane name="all">
          <template #label>
            <el-badge :value="allCount" :hidden="allCount === 0">
              全部消息
            </el-badge>
          </template>
          <div class="message-filters">
            <el-select v-model="filters.type" placeholder="消息类型" style="width: 150px; margin-right: 16px">
              <el-option label="全部" value="" />
              <el-option label="系统通知" value="system" />
              <el-option label="学习提醒" value="learning" />
              <el-option label="私信" value="private" />
              <el-option label="作业反馈" value="feedback" />
            </el-select>
            <el-select v-model="filters.status" placeholder="消息状态" style="width: 120px">
              <el-option label="全部" value="" />
              <el-option label="未读" value="unread" />
              <el-option label="已读" value="read" />
            </el-select>
          </div>
          <MessageList
            :messages="filteredMessages"
            :loading="loading"
            @message-click="viewMessage"
            @message-delete="deleteMessage"
          />
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            layout="total, prev, pager, next"
            style="margin-top: 16px; text-align: center"
          />
        </el-tab-pane>

        <el-tab-pane name="system">
          <template #label>
            <el-badge :value="systemCount" :hidden="systemCount === 0">
              系统通知
            </el-badge>
          </template>
          <MessageList
            :messages="systemMessages"
            :loading="loading"
            @message-click="viewMessage"
            @message-delete="deleteMessage"
          />
        </el-tab-pane>

        <el-tab-pane name="learning">
          <template #label>
            <el-badge :value="learningCount" :hidden="learningCount === 0">
              学习提醒
            </el-badge>
          </template>
          <MessageList
            :messages="learningMessages"
            :loading="loading"
            @message-click="viewMessage"
            @message-delete="deleteMessage"
          />
        </el-tab-pane>

        <el-tab-pane name="private">
          <template #label>
            <el-badge :value="privateCount" :hidden="privateCount === 0">
              私信
            </el-badge>
          </template>
          <div class="private-messages">
            <el-row :gutter="20">
              <el-col :span="8" class="conversation-list">
                <div
                  v-for="conversation in conversations"
                  :key="conversation.id"
                  class="conversation-item"
                  :class="{ active: selectedConversation?.id === conversation.id }"
                  @click="selectConversation(conversation)"
                >
                  <el-badge :value="conversation.unread" :hidden="conversation.unread === 0" class="message-badge">
                    <el-avatar :size="40" :src="conversation.avatar">
                      {{ conversation.name.charAt(0) }}
                    </el-avatar>
                  </el-badge>
                  <div class="conversation-info">
                    <div class="conversation-header">
                      <span class="conversation-name">{{ conversation.name }}</span>
                      <span class="conversation-time">{{ conversation.time }}</span>
                    </div>
                    <div class="conversation-message">{{ conversation.lastMessage }}</div>
                  </div>
                </div>
              </el-col>
              <el-col :span="16" class="chat-area">
                <div v-if="selectedConversation" class="chat-container">
                  <div class="chat-header">
                    <span>{{ selectedConversation.name }}</span>
                    <el-button link @click="selectedConversation = null">关闭</el-button>
                  </div>
                  <div class="chat-messages" ref="chatMessagesRef">
                    <div
                      v-for="(msg, index) in selectedConversation.messages"
                      :key="index"
                      class="chat-message"
                      :class="{ 'my-message': msg.isMine }"
                    >
                      <el-avatar :size="36" :src="msg.isMine ? userInfo?.avatar : selectedConversation.avatar">
                        {{ (msg.isMine ? userInfo?.name : selectedConversation.name)?.charAt(0) }}
                      </el-avatar>
                      <div class="message-bubble">
                        <div class="message-content">{{ msg.content }}</div>
                        <div class="message-time">{{ msg.time }}</div>
                      </div>
                    </div>
                  </div>
                  <div class="chat-input">
                    <el-input
                      v-model="newMessage"
                      type="textarea"
                      :rows="3"
                      placeholder="输入消息..."
                      @keydown.enter.ctrl="sendMessage"
                    />
                    <div class="input-actions">
                      <span class="input-hint">按 Ctrl+Enter 发送</span>
                      <el-button type="primary" @click="sendMessage" :disabled="!newMessage.trim()">
                        发送
                      </el-button>
                    </div>
                  </div>
                </div>
                <div v-else class="chat-placeholder">
                  <el-empty description="选择一个会话开始聊天" />
                </div>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <el-tab-pane name="feedback">
          <template #label>
            <el-badge :value="feedbackCount" :hidden="feedbackCount === 0">
              作业反馈
            </el-badge>
          </template>
          <MessageList
            :messages="feedbackMessages"
            :loading="loading"
            @message-click="viewMessage"
            @message-delete="deleteMessage"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog
      v-model="messageDetailVisible"
      :title="selectedMessage?.title"
      width="600px"
    >
      <div v-if="selectedMessage" class="message-detail">
        <div class="detail-header">
          <el-tag :type="getTypeTag(selectedMessage.type)">
            {{ getTypeLabel(selectedMessage.type) }}
          </el-tag>
          <span class="detail-time">{{ selectedMessage.time }}</span>
        </div>
        <div class="detail-content">
          <p>{{ selectedMessage.content }}</p>
        </div>
        <div v-if="selectedMessage.attachment" class="detail-attachment">
          <el-divider />
          <div class="attachment-info">
            <el-icon><Paperclip /></el-icon>
            <span>{{ selectedMessage.attachment.name }}</span>
            <el-button link type="primary" @click="downloadAttachment(selectedMessage.attachment)">
              下载
            </el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="messageDetailVisible = false">关闭</el-button>
        <el-button v-if="selectedMessage && !selectedMessage.read" type="primary" @click="markAsRead">
          标记为已读
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Paperclip } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

interface Message {
  id: number
  title: string
  content: string
  type: string
  read: boolean
  time: string
  attachment?: {
    name: string
    url: string
  }
}

interface Conversation {
  id: number
  name: string
  avatar: string
  lastMessage: string
  time: string
  unread: number
  messages: ChatMessage[]
}

interface ChatMessage {
  content: string
  time: string
  isMine: boolean
}

const MessageList = {
  props: ['messages', 'loading'],
  emits: ['messageClick', 'messageDelete'],
  template: `
    <el-skeleton :loading="loading" :count="5" animated>
      <template #template>
        <el-skeleton-item variant="rect" style="height: 80px; margin-bottom: 16px" />
      </template>
      <template #default>
        <div v-if="messages.length === 0" class="empty-messages">
          <el-empty description="暂无消息" />
        </div>
        <div v-else class="message-list">
          <div
            v-for="message in messages"
            :key="message.id"
            class="message-item"
            :class="{ unread: !message.read }"
            @click="$emit('messageClick', message)"
          >
            <div class="message-main">
              <div class="message-header">
                <div class="message-title">{{ message.title }}</div>
                <div class="message-time">{{ message.time }}</div>
              </div>
              <div class="message-content-preview">{{ message.content }}</div>
              <div class="message-footer">
                <el-tag :type="getTypeTag(message.type)" size="small">
                  {{ getTypeLabel(message.type) }}
                </el-tag>
                <el-button
                  link
                  type="danger"
                  size="small"
                  @click.stop="$emit('messageDelete', message)"
                >
                  删除
                </el-button>
              </div>
            </div>
            <div v-if="!message.read" class="unread-dot"></div>
          </div>
        </div>
      </template>
    </el-skeleton>
  `,
  methods: {
    getTypeTag(type: string) {
      const tags: Record<string, any> = {
        system: 'danger',
        learning: 'warning',
        private: 'success',
        feedback: 'info'
      }
      return tags[type] || 'info'
    },
    getTypeLabel(type: string) {
      const labels: Record<string, string> = {
        system: '系统通知',
        learning: '学习提醒',
        private: '私信',
        feedback: '作业反馈'
      }
      return labels[type] || type
    }
  }
}

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const activeTab = ref('all')
const loading = ref(false)
const messageDetailVisible = ref(false)
const selectedMessage = ref<Message | null>(null)
const selectedConversation = ref<Conversation | null>(null)
const newMessage = ref('')
const chatMessagesRef = ref()

const filters = reactive({
  type: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 50
})

const messages = ref<Message[]>([
  {
    id: 1,
    title: '系统升级通知',
    content: '系统将于今晚23:00-01:00进行升级维护，届时将暂停服务，请提前做好准备。',
    type: 'system',
    read: false,
    time: '2026-05-02 14:30'
  },
  {
    id: 2,
    title: '学习任务提醒',
    content: '您有3个学习任务即将到期，请及时完成：1. Vue组件练习 2. Spring Boot基础 3. 数据库设计',
    type: 'learning',
    read: false,
    time: '2026-05-02 12:00'
  },
  {
    id: 3,
    title: '作业批改完成',
    content: '您的"电商管理系统前端开发"作业已批改完成，得分：85分。请查看详细评语和改进建议。',
    type: 'feedback',
    read: true,
    time: '2026-05-01 18:20',
    attachment: {
      name: '作业评语.pdf',
      url: '/files/feedback.pdf'
    }
  },
  {
    id: 4,
    title: '新课程上线',
    content: '【高级】React性能优化实战课程已上线，原价399元，现在报名立减100元！',
    type: 'system',
    read: true,
    time: '2026-04-30 10:00'
  },
  {
    id: 5,
    title: '学习计划完成',
    content: '恭喜您完成本月学习计划！您的前端开发能力已达到中级水平，可以尝试进阶课程。',
    type: 'learning',
    read: false,
    time: '2026-04-29 09:30'
  }
])

const conversations = ref<Conversation[]>([
  {
    id: 1,
    name: '李老师',
    avatar: '',
    lastMessage: '好的，我们明天再详细讨论项目需求。',
    time: '14:20',
    unread: 2,
    messages: [
      { content: '同学你好，关于你的项目我有一些建议。', time: '14:10', isMine: false },
      { content: '老师您好，请问有什么建议？', time: '14:15', isMine: true },
      { content: '首先，你的代码结构很好，但是在错误处理方面还可以改进。', time: '14:16', isMine: false },
      { content: '好的，我明白了。请问具体应该怎么改呢？', time: '14:18', isMine: true },
      { content: '好的，我们明天再详细讨论项目需求。', time: '14:20', isMine: false }
    ]
  },
  {
    id: 2,
    name: '张同学',
    avatar: '',
    lastMessage: '那个组件的实现方法确实很不错！',
    time: '昨天',
    unread: 0,
    messages: [
      { content: '你好，看到你做的Vue组件很棒！', time: '昨天 15:00', isMine: false },
      { content: '谢谢！只是基础的组件练习而已。', time: '昨天 15:10', isMine: true },
      { content: '那个组件的实现方法确实很不错！', time: '昨天 15:15', isMine: false }
    ]
  },
  {
    id: 3,
    name: '王助教',
    avatar: '',
    lastMessage: '你的作业批改结果已经出来了。',
    time: '2天前',
    unread: 1,
    messages: [
      { content: '你的作业批改结果已经出来了。', time: '2天前', isMine: false }
    ]
  }
])

const filteredMessages = computed(() => {
  return messages.value.filter(msg => {
    const typeMatch = !filters.type || msg.type === filters.type
    const statusMatch = !filters.status ||
      (filters.status === 'unread' && !msg.read) ||
      (filters.status === 'read' && msg.read)
    return typeMatch && statusMatch
  })
})

const systemMessages = computed(() => messages.value.filter(msg => msg.type === 'system'))
const learningMessages = computed(() => messages.value.filter(msg => msg.type === 'learning'))
const privateMessages = computed(() => messages.value.filter(msg => msg.type === 'private'))
const feedbackMessages = computed(() => messages.value.filter(msg => msg.type === 'feedback'))

const unreadCount = computed(() => messages.value.filter(msg => !msg.read).length)
const allCount = computed(() => messages.value.length)
const systemCount = computed(() => systemMessages.value.filter(msg => !msg.read).length)
const learningCount = computed(() => learningMessages.value.filter(msg => !msg.read).length)
const privateCount = computed(() => privateMessages.value.filter(msg => !msg.read).length)
const feedbackCount = computed(() => feedbackMessages.value.filter(msg => !msg.read).length)

const getTypeTag = (type: string) => {
  const tags: Record<string, any> = {
    system: 'danger',
    learning: 'warning',
    private: 'success',
    feedback: 'info'
  }
  return tags[type] || 'info'
}

const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    system: '系统通知',
    learning: '学习提醒',
    private: '私信',
    feedback: '作业反馈'
  }
  return labels[type] || type
}

const viewMessage = (message: Message) => {
  selectedMessage.value = message
  messageDetailVisible.value = true
}

const deleteMessage = (message: Message) => {
  ElMessageBox.confirm('确定要删除这条消息吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const index = messages.value.findIndex(m => m.id === message.id)
    if (index > -1) {
      messages.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  }).catch(() => {})
}

const markAsRead = () => {
  if (selectedMessage.value) {
    selectedMessage.value.read = true
    ElMessage.success('已标记为已读')
  }
}

const markAllRead = () => {
  messages.value.forEach(msg => msg.read = true)
  conversations.value.forEach(conv => conv.unread = 0)
  ElMessage.success('已全部标记为已读')
}

const clearMessages = () => {
  ElMessageBox.confirm('确定要清空所有消息吗？此操作不可恢复！', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    messages.value = []
    ElMessage.success('消息已清空')
  }).catch(() => {})
}

const selectConversation = (conversation: Conversation) => {
  selectedConversation.value = conversation
  conversation.unread = 0
  nextTick(() => {
    scrollToBottom()
  })
}

const sendMessage = () => {
  if (!newMessage.value.trim()) return

  const now = new Date()
  const time = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`

  selectedConversation.value?.messages.push({
    content: newMessage.value,
    time,
    isMine: true
  })

  selectedConversation.value!.lastMessage = newMessage.value
  selectedConversation.value!.time = time

  newMessage.value = ''

  nextTick(() => {
    scrollToBottom()
  })

  ElMessage.success('消息已发送')
}

const scrollToBottom = () => {
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
  }
}

const downloadAttachment = (attachment: any) => {
  ElMessage.success(`正在下载：${attachment.name}`)
}
</script>

<style scoped>
.messages-page {
  padding: 16px;
}

.messages-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.unread-badge {
  font-size: 14px;
  color: #666;
}

.message-filters {
  display: flex;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.empty-messages {
  padding: 64px 0;
  text-align: center;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.message-item:hover {
  background: #e8f4ff;
  transform: translateX(5px);
}

.message-item.unread {
  background: #fff;
  border-left: 4px solid #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.message-main {
  flex: 1;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.message-title {
  font-weight: bold;
  color: #333;
  font-size: 16px;
}

.message-time {
  color: #999;
  font-size: 14px;
}

.message-content-preview {
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: #409eff;
  border-radius: 50%;
  margin-left: 16px;
}

.private-messages {
  padding: 16px 0;
}

.conversation-list {
  max-height: 500px;
  overflow-y: auto;
  border-right: 1px solid #eee;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  cursor: pointer;
  transition: background 0.3s;
  border-bottom: 1px solid #f0f0f0;
}

.conversation-item:hover {
  background: #f5f7fa;
}

.conversation-item.active {
  background: #e8f4ff;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.conversation-name {
  font-weight: bold;
  color: #333;
}

.conversation-time {
  font-size: 12px;
  color: #999;
}

.conversation-message {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-area {
  display: flex;
  flex-direction: column;
  height: 500px;
}

.chat-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #eee;
  font-weight: bold;
  color: #333;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
}

.chat-message {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.chat-message.my-message {
  flex-direction: row-reverse;
}

.message-bubble {
  max-width: 60%;
}

.message-content {
  padding: 12px 16px;
  border-radius: 12px;
  background: #fff;
  color: #333;
  line-height: 1.6;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.chat-message.my-message .message-content {
  background: #409eff;
  color: #fff;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  text-align: right;
}

.chat-input {
  padding: 16px;
  border-top: 1px solid #eee;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.input-hint {
  font-size: 12px;
  color: #999;
}

.message-detail {
  padding: 8px 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.detail-time {
  color: #999;
  font-size: 14px;
}

.detail-content {
  line-height: 1.8;
  color: #333;
  font-size: 15px;
}

.detail-attachment {
  margin-top: 16px;
}

.attachment-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 6px;
}
</style>
