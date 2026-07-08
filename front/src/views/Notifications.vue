<template>
  <div class="notifications-page">
    <div class="container">
      <div class="page-header">
        <h2 class="page-title">通知</h2>
        <button v-if="notifications.length" class="btn btn-outline btn-sm" @click="handleReadAll">全部已读</button>
      </div>

      <LoadingSpinner v-if="loading" />
      <EmptyState v-else-if="!notifications.length" icon="🔔" title="暂无通知" />

      <div v-else class="notif-list">
        <div
          v-for="n in notifications"
          :key="n.id"
          :class="['notif-item card', { unread: !n.isRead }]"
          @click="handleClick(n)"
        >
          <div class="notif-icon">{{ typeIcon(n.type) }}</div>
          <div class="notif-info">
            <div class="notif-header">
              <h4 class="notif-title">{{ n.title }}</h4>
              <span v-if="!n.isRead" class="unread-dot"></span>
            </div>
            <p class="notif-content">{{ n.content }}</p>
            <span class="notif-time">{{ formatTime(n.createdAt) }}</span>
          </div>
        </div>
      </div>

      <Pagination :current-page="page" :total-pages="totalPages" :total="total" @change="changePage" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getNotifications, markNotificationRead, markAllNotificationsRead } from '@/api/notification'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const router = useRouter()

const notifications = ref([])
const page = ref(0)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)

function typeIcon(t) {
  const map = {
    PRODUCT_APPROVED: '✅', PRODUCT_REJECTED: '❌', PRODUCT_OFF_SHELF: '📦',
    ORDER_NEW: '🛒', ORDER_SHIPPED: '🚚', ORDER_CANCELLED: '❌',
    ORDER_COMPLETED: '🎉', SYSTEM: '📢'
  }
  return map[t] || '🔔'
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return d.toLocaleDateString('zh-CN')
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getNotifications({ page: page.value, size: 20 })
    notifications.value = res.list || []
    total.value = res.total || 0
    totalPages.value = res.totalPages || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function changePage(p) { page.value = p; fetchData() }

async function handleClick(n) {
  if (!n.isRead) {
    try { await markNotificationRead(n.id); n.isRead = true }
    catch { /* ignore */ }
  }
  // Navigate based on type
  if (n.relatedId) {
    if (n.type?.startsWith('ORDER')) {
      router.push(`/order/${n.relatedId}`)
    } else if (n.type?.startsWith('PRODUCT')) {
      router.push(`/product/${n.relatedId}`)
    }
  }
}

async function handleReadAll() {
  try {
    await markAllNotificationsRead()
    notifications.value.forEach(n => n.isRead = true)
    window.$toast?.success('已全部标为已读')
  } catch { /* ignore */ }
}

onMounted(fetchData)
</script>

<style scoped>
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px;
}

.page-title { font-size: 22px; font-weight: 600; }

.notif-list { display: flex; flex-direction: column; gap: 8px; }

.notif-item {
  display: flex; gap: 14px; padding: 16px;
  cursor: pointer; transition: background 0.2s;
}

.notif-item:hover { background: var(--bg); }
.notif-item.unread { background: #F1F8E9; border-left: 3px solid var(--primary); }

.notif-icon { font-size: 24px; flex-shrink: 0; }

.notif-info { flex: 1; min-width: 0; }

.notif-header { display: flex; align-items: center; gap: 8px; }

.notif-title { font-size: 15px; font-weight: 500; }

.unread-dot {
  width: 8px; height: 8px;
  background: var(--primary); border-radius: 50%;
}

.notif-content {
  font-size: 13px; color: var(--text-secondary);
  margin-top: 4px; line-height: 1.5;
}

.notif-time { font-size: 12px; color: var(--text-muted); margin-top: 6px; display: block; }
</style>
