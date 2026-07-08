<template>
  <div class="chat-list-page">
    <div class="container">
      <h2 class="page-title">消息</h2>
      <LoadingSpinner v-if="loading" />
      <EmptyState v-else-if="!conversations.length" icon="💬" title="暂无消息" description="浏览商品时点击「联系卖家」即可开始对话" />
      <div v-else class="conversation-list">
        <router-link
          v-for="conv in conversations"
          :key="conv.id"
          :to="`/chat/${conv.id}`"
          class="conv-item card"
        >
          <div class="conv-avatar">
            <img v-if="conv.otherUserAvatar" :src="conv.otherUserAvatar" alt="" />
            <span v-else class="avatar-placeholder">👤</span>
          </div>
          <div class="conv-info">
            <div class="conv-header">
              <span class="conv-name">{{ conv.otherUserName || '用户' }}</span>
              <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
            </div>
            <div class="conv-preview">
              <span class="conv-msg">{{ conv.lastMessage || '暂无消息' }}</span>
              <span v-if="conv.unreadCount" class="unread-badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
            </div>
            <div v-if="conv.productTitle" class="conv-product">
              <img v-if="conv.productImage" :src="conv.productImage" class="product-thumb" />
              <span>来自：{{ conv.productTitle }}</span>
            </div>
          </div>
        </router-link>
      </div>
      <Pagination :current-page="page" :total-pages="totalPages" :total="total" @change="changePage" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getConversations } from '@/api/chat'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const conversations = ref([])
const page = ref(0)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return d.toLocaleDateString('zh-CN')
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getConversations({ page: page.value, size: 20 })
    conversations.value = res.list || []
    total.value = res.total || 0
    totalPages.value = res.totalPages || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function changePage(p) { page.value = p; fetchData() }

onMounted(fetchData)
</script>

<style scoped>
.page-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; }

.conversation-list {
  display: flex; flex-direction: column; gap: 2px;
  background: var(--bg-white); border-radius: var(--radius); box-shadow: var(--shadow);
  overflow: hidden;
}

.conv-item {
  display: flex; gap: 12px; padding: 14px 16px;
  border-radius: 0; box-shadow: none;
  transition: background 0.2s;
  border-bottom: 1px solid var(--border-light);
}

.conv-item:last-child { border-bottom: none; }
.conv-item:hover { background: var(--bg); }

.conv-avatar { flex-shrink: 0; }
.conv-avatar img {
  width: 48px; height: 48px; border-radius: 50%; object-fit: cover;
}

.avatar-placeholder { font-size: 32px; }

.conv-info { flex: 1; min-width: 0; }

.conv-header {
  display: flex; justify-content: space-between; align-items: center;
}

.conv-name { font-size: 16px; font-weight: 500; }
.conv-time { font-size: 12px; color: var(--text-muted); }

.conv-preview {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 2px;
}

.conv-msg {
  font-size: 13px; color: var(--text-muted);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  max-width: 300px;
}

.unread-badge {
  min-width: 20px; height: 20px;
  background: var(--danger); color: #fff;
  font-size: 11px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  padding: 0 6px;
}

.conv-product {
  display: flex; align-items: center; gap: 6px;
  margin-top: 6px; font-size: 12px; color: var(--text-muted);
}

.product-thumb {
  width: 24px; height: 24px; border-radius: 4px; object-fit: cover;
}
</style>
