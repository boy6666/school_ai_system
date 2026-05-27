<template>
  <div class="tutor-chat">
    <div class="chat-messages" ref="messagesContainer">
      <div v-for="(msg, idx) in messages" :key="idx" :class="['message', msg.role]">
        <div class="content">{{ msg.content }}</div>
      </div>
      <div v-if="loading" class="message assistant">正在输入...</div>
    </div>
    <div class="input-area">
      <el-input v-model="inputText" type="textarea" :rows="2" placeholder="输入问题..." />
      <el-button type="primary" @click="send" :loading="loading">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { sendMessage } from '@/api/tutor'

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const messagesContainer = ref(null)

const send = async () => {
  if (!inputText.value.trim()) return
  messages.value.push({ role: 'user', content: inputText.value })
  const question = inputText.value
  inputText.value = ''
  loading.value = true
  await nextTick()
  scrollToBottom()
  try {
    const res = await sendMessage({ message: question })
    if (res.code === 200) {
      messages.value.push({ role: 'assistant', content: res.data.finalAnswer })
    } else {
      ElMessage.error(res.message || '请求失败')
    }
  } catch (error) {
    ElMessage.error('网络错误')
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}
</script>

<style scoped>
.tutor-chat { display: flex; flex-direction: column; height: 80vh; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; background: #f5f7fa; }
.message { margin-bottom: 12px; }
.message.user { text-align: right; }
.message.user .content { background: #409eff; color: white; display: inline-block; padding: 8px 12px; border-radius: 12px; }
.message.assistant .content { background: white; display: inline-block; padding: 8px 12px; border-radius: 12px; }
.input-area { display: flex; gap: 12px; padding: 12px; background: white; }
</style>
