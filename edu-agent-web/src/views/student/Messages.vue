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
            <el-select v-model="filters.type" placeholder="消息类型" style="width: 150px; margin-right: 15px" @change="handleFilterChange">
              <el-option label="全部" value="" />
              <el-option label="系统通知" value="system" />
              <el-option label="学习提醒" value="learning" />
              <el-option label="私信" value="private" />
              <el-option label="作业反馈" value="feedback" />
            </el-select>
            <el-select v-model="filters.status" placeholder="消息状态" style="width: 150px" @change="handleFilterChange">
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
            style="margin-top: 20px; text-align: center"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
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
                      <el-button type="primary" @click="sendMessage" :disabled="!newMessage.trim()" :loading="sending">
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
import { computed, nextTick, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Paperclip } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  deleteMessage as deleteMessageApi,
  getConversationList,
  getConversationMessages,
  getMessageDetail,
  getMessageList,
  markAllRead as markAllReadApi,
  markAsRead as markAsReadApi,
  sendMessage as sendMessageApi,
  type ChatMessage,
  type ConversationItem,
  type MessageAttachment,
  type MessageItem
} from '@/api/message'

type TagType = 'success' | 'warning' | 'danger' | 'info'
type ConversationView = ConversationItem & { messages: ChatMessage[] }

const typeTags: Record<string, TagType> = {
  system: 'danger', learning: 'warning', private: 'success', feedback: 'info'
}
const typeLabels: Record<string, string> = {
  system: '系统通知', learning: '学习提醒', private: '私信', feedback: '作业反馈'
}

const MessageList = {
  props: ['messages', 'loading'],
  emits: ['messageClick', 'messageDelete'],
  template: `
    <el-skeleton :loading="loading" :count="5" animated>
      <template #template>
        <el-skeleton-item variant="rect" style="height:80px;margin-bottom:15px" />
      </template>
      <template #default>
        <el-empty v-if="messages.length === 0" description="暂无消息" />
        <div v-else class="message-list">
          <div v-for="message in messages" :key="message.id" class="message-item"
            :class="{ unread: !message.read }" @click="$emit('messageClick', message)">
            <div class="message-main">
              <div class="message-header">
                <div class="message-title">{{ message.title }}</div>
                <div class="message-time">{{ message.time }}</div>
              </div>
              <div class="message-content-preview">{{ message.content }}</div>
              <div class="message-footer">
                <el-tag :type="getTypeTag(message.type)" size="small">{{ getTypeLabel(message.type) }}</el-tag>
                <el-button link type="danger" size="small"
                  @click.stop="$emit('messageDelete', message)">删除</el-button>
              </div>
            </div>
            <div v-if="!message.read" class="unread-dot"></div>
          </div>
        </div>
      </template>
    </el-skeleton>
  `,
  methods: {
    getTypeTag: (type: string): TagType => typeTags[type] || 'info',
    getTypeLabel: (type: string): string => typeLabels[type] || type
  }
}

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const activeTab = ref('all')
const loading = ref(false)
const sending = ref(false)
const messageDetailVisible = ref(false)
const selectedMessage = ref<MessageItem | null>(null)
const selectedConversation = ref<ConversationView | null>(null)
const newMessage = ref('')
const chatMessagesRef = ref<HTMLElement>()
const filters = reactive({ type: '', status: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })
const messages = ref<MessageItem[]>([])
const conversations = ref<ConversationView[]>([])

const filteredMessages = computed(() => messages.value)
const systemMessages = computed(() => messages.value.filter((message) => message.type === 'system'))
const learningMessages = computed(() => messages.value.filter((message) => message.type === 'learning'))
const privateMessages = computed(() => messages.value.filter((message) => message.type === 'private'))
const feedbackMessages = computed(() => messages.value.filter((message) => message.type === 'feedback'))
const unreadCount = computed(() => messages.value.filter((message) => !message.read).length)
const allCount = computed(() => messages.value.length)
const systemCount = computed(() => systemMessages.value.filter((message) => !message.read).length)
const learningCount = computed(() => learningMessages.value.filter((message) => !message.read).length)
const privateCount = computed(() => privateMessages.value.filter((message) => !message.read).length)
const feedbackCount = computed(() => feedbackMessages.value.filter((message) => !message.read).length)
const getTypeTag = (type: string): TagType => typeTags[type] || 'info'
const getTypeLabel = (type: string): string => typeLabels[type] || type

const loadMessages = async () => {
  loading.value = true
  try {
    const result = await getMessageList({
      type: filters.type || undefined,
      status: filters.status || undefined,
      page: pagination.page,
      pageSize: pagination.size
    })
    messages.value = result.records
    pagination.total = result.total
  } catch {
    messages.value = []
    pagination.total = 0
    ElMessage.error('消息列表加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const loadConversations = async () => {
  try {
    const result = await getConversationList()
    conversations.value = result.map((item) => ({ ...item, messages: item.messages || [] }))
  } catch {
    conversations.value = []
    ElMessage.error('私信会话加载失败，请稍后重试')
  }
}

const handleFilterChange = () => { pagination.page = 1; loadMessages() }
const handleSizeChange = (size: number) => { pagination.size = size; loadMessages() }
const handlePageChange = (page: number) => { pagination.page = page; loadMessages() }

const viewMessage = async (message: MessageItem) => {
  try {
    selectedMessage.value = await getMessageDetail(message.id)
    messageDetailVisible.value = true
  } catch {
    ElMessage.error('消息详情加载失败，请稍后重试')
  }
}

const deleteMessage = async (message: MessageItem) => {
  try {
    await ElMessageBox.confirm('确定要删除这条消息吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
  } catch { return }
  try {
    await deleteMessageApi(message.id)
    ElMessage.success('删除成功')
    await loadMessages()
  } catch {
    ElMessage.error('删除失败，请稍后重试')
  }
}

const markAsRead = async () => {
  if (!selectedMessage.value) return
  try {
    await markAsReadApi(selectedMessage.value.id)
    selectedMessage.value.read = true
    ElMessage.success('已标记为已读')
    await loadMessages()
  } catch {
    ElMessage.error('标记已读失败，请稍后重试')
  }
}

const markAllRead = async () => {
  try {
    await markAllReadApi()
    ElMessage.success('已全部标记为已读')
    await Promise.all([loadMessages(), loadConversations()])
  } catch {
    ElMessage.error('批量标记失败，请稍后重试')
  }
}

const clearMessages = () => ElMessage.info('清空消息接口暂未开放')

const selectConversation = async (conversation: ConversationView) => {
  try {
    const history = await getConversationMessages(conversation.id)
    selectedConversation.value = { ...conversation, messages: history, unread: 0 }
    await nextTick()
    scrollToBottom()
  } catch {
    ElMessage.error('会话记录加载失败，请稍后重试')
  }
}

const sendMessage = async () => {
  const content = newMessage.value.trim()
  if (!content || !selectedConversation.value || sending.value) return
  sending.value = true
  try {
    const sent = await sendMessageApi({
      conversationId: selectedConversation.value.id,
      content
    })
    selectedConversation.value.messages.push(sent)
    selectedConversation.value.lastMessage = sent.content
    selectedConversation.value.time = sent.time
    newMessage.value = ''
    await nextTick()
    scrollToBottom()
    ElMessage.success('消息已发送')
  } catch {
    ElMessage.error('消息发送失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

const scrollToBottom = () => {
  if (chatMessagesRef.value) chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
}

const downloadAttachment = (attachment: MessageAttachment) => {
  if (!attachment.url) { ElMessage.info('附件下载地址暂不可用'); return }
  window.open(attachment.url, '_blank', 'noopener,noreferrer')
}

loadMessages()
loadConversations()
</script>

<style scoped>
.messages-page {
  padding: 20px;
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
  gap: 20px;
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
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.empty-messages {
  padding: 60px 0;
  text-align: center;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message-item {
  display: flex;
  align-items: center;
  padding: 20px;
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
  margin-bottom: 10px;
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
  margin-bottom: 10px;
  line-height: 1.6;
display: -webkit-box;
line-clamp: 2;
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
  margin-left: 15px;
}

.private-messages {
  padding: 20px 0;
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
  padding: 15px;
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
  margin-bottom: 5px;
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
  padding: 15px;
  border-bottom: 1px solid #eee;
  font-weight: bold;
  color: #333;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;
}

.chat-message {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
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
  margin-top: 5px;
  text-align: right;
}

.chat-input {
  padding: 15px;
  border-top: 1px solid #eee;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.input-hint {
  font-size: 12px;
  color: #999;
}

.message-detail {
  padding: 10px 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
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
  margin-top: 20px;
}

.attachment-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 6px;
}
</style>