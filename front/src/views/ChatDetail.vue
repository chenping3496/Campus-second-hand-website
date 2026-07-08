<template>
  <div class="chat-detail-page">
    <div class="chat-container">
      <!-- Header -->
      <div class="chat-header">
        <router-link to="/chat" class="back-btn">← 返回</router-link>
        <div class="chat-user" v-if="conversation">
          <img v-if="conversation.otherUserAvatar" :src="conversation.otherUserAvatar" class="user-avatar" />
          <span v-else class="avatar-placeholder">👤</span>
          <span class="user-name">{{ conversation.otherUserName || '用户' }}</span>
        </div>
        <router-link
          v-if="conversation?.productId"
          :to="`/product/${conversation.productId}`"
          class="product-ref"
        >
          查看商品
        </router-link>
      </div>

      <!-- Messages -->
      <div class="chat-messages" ref="msgContainer">
        <LoadingSpinner v-if="loading" />
        <EmptyState v-else-if="!messages.length" icon="💬" title="开始对话" description="发送第一条消息吧" />

        <div v-else class="msg-list">
          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="['msg-item', { 'msg-mine': msg.senderId === auth.user?.id }]"
          >
            <div class="msg-bubble">
              <p class="msg-text">{{ msg.content }}</p>
              <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Input -->
      <div class="chat-input-bar">
        <input
          v-model="inputText"
          type="text"
          class="chat-input"
          placeholder="输入消息..."
          @keyup.enter="handleSend"
          :disabled="sending"
        />
        <button class="btn btn-primary" @click="handleSend" :disabled="!inputText.trim() || sending">
          {{ sending ? '发送中' : '发送' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getConversation, getMessages, sendMessage, markAsRead } from '@/api/chat'
import { useAuthStore } from '@/stores/auth'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const auth = useAuthStore()

const conversation = ref(null)
const messages = ref([])
const inputText = ref('')
const loading = ref(true)
const sending = ref(false)
const msgContainer = ref(null)
const page = ref(0)
const totalPages = ref(0)
let ws = null

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function fetchConversation() {
  try {
    conversation.value = await getConversation(route.params.id)
    await markAsRead(route.params.id)
  } catch { /* ignore */ }
}

async function fetchMessages() {
  try {
    const res = await getMessages(route.params.id, { page: 0, size: 100 })
    messages.value = (res.list || []).reverse()
    totalPages.value = res.totalPages || 0
    await nextTick()
    scrollToBottom()
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function scrollToBottom() {
  if (msgContainer.value) {
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  }
}

function connectWebSocket() {
  const token = localStorage.getItem('token')
  if (!token) return
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${window.location.host}/ws/chat?token=${token}&conversationId=${route.params.id}`

  ws = new WebSocket(wsUrl)
  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      if (msg.type === 'message') {
        messages.value.push(msg)
        nextTick(() => scrollToBottom())
      }
    } catch { /* ignore */ }
  }
  ws.onerror = () => { /* ignore */ }
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  sending.value = true
  inputText.value = ''
  try {
    const msg = await sendMessage(route.params.id, { content: text, type: 'TEXT' })
    messages.value.push(msg)
    await nextTick()
    scrollToBottom()
  } catch (err) {
    window.$toast?.error(err.message || '发送失败')
    inputText.value = text
  } finally {
    sending.value = false
  }
}

watch(() => route.params.id, () => {
  fetchConversation()
  fetchMessages()
  connectWebSocket()
})

onMounted(() => {
  fetchConversation()
  fetchMessages()
  connectWebSocket()
})

onUnmounted(() => {
  if (ws) ws.close()
})
</script>

<style scoped>
.chat-detail-page {
  height: calc(100vh - var(--header-height) - 80px);
  display: flex; flex-direction: column;
}

.chat-container {
  display: flex; flex-direction: column;
  height: 100%; max-width: 700px; margin: 0 auto;
  width: 100%;
  background: var(--bg-white);
  box-shadow: var(--shadow);
}

.chat-header {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; border-bottom: 1px solid var(--border-light);
  background: var(--bg-white);
}

.back-btn { font-size: 14px; color: var(--text-secondary); }
.back-btn:hover { color: var(--primary); }

.chat-user { display: flex; align-items: center; gap: 8px; flex: 1; }
.user-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; }
.avatar-placeholder { font-size: 24px; }
.user-name { font-size: 16px; font-weight: 500; }

.product-ref { font-size: 13px; color: var(--primary); }

.chat-messages { flex: 1; overflow-y: auto; padding: 16px; background: var(--bg); }

.msg-list { display: flex; flex-direction: column; gap: 12px; }

.msg-item { display: flex; }

.msg-mine { justify-content: flex-end; }

.msg-bubble {
  max-width: 70%; padding: 10px 14px;
  border-radius: 16px; position: relative;
}

.msg-item:not(.msg-mine) .msg-bubble {
  background: var(--bg-white); border-top-left-radius: 4px;
}

.msg-mine .msg-bubble {
  background: var(--primary); color: #fff; border-top-right-radius: 4px;
}

.msg-text { font-size: 15px; line-height: 1.5; word-break: break-word; }
.msg-time { font-size: 11px; opacity: 0.6; margin-top: 4px; display: block; }
.msg-mine .msg-time { text-align: right; }

.chat-input-bar {
  display: flex; gap: 10px;
  padding: 12px 16px; border-top: 1px solid var(--border-light);
  background: var(--bg-white);
}

.chat-input {
  flex: 1; padding: 10px 14px;
  border: 1px solid var(--border); border-radius: 20px;
  font-size: 14px;
}

.chat-input:focus { border-color: var(--primary); }

@media (max-width: 768px) {
  .chat-container { max-width: 100%; }
}
</style>
