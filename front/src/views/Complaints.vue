<template>
  <div class="complaints-page">
    <div class="container">
      <div class="page-header-row">
        <h2 class="page-title">我的投诉</h2>
        <button class="btn btn-primary" @click="showForm = true">提交投诉</button>
      </div>

      <!-- Submit form dialog -->
      <div v-if="showForm" class="card complaint-form-card">
        <h3>提交投诉</h3>
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>投诉类型 *</label>
            <select v-model="form.type" class="form-input">
              <option value="USER">用户行为</option>
              <option value="PRODUCT">商品问题</option>
              <option value="ORDER">订单问题</option>
              <option value="OTHER">其他</option>
            </select>
          </div>
          <div class="form-group">
            <label>投诉标题 *</label>
            <input v-model="form.title" type="text" class="form-input" placeholder="简要描述投诉内容" />
          </div>
          <div class="form-group">
            <label>投诉详情 *</label>
            <textarea v-model="form.content" class="form-textarea" rows="5" placeholder="请详细描述您遇到的问题..."></textarea>
          </div>
          <div class="form-group">
            <label>关联订单（选填）</label>
            <input v-model.number="form.orderId" type="number" class="form-input" placeholder="如有相关订单，输入订单ID" />
          </div>
          <div class="form-group">
            <label>被投诉用户ID（选填）</label>
            <input v-model.number="form.targetUserId" type="number" class="form-input" placeholder="投诉特定用户时填写" />
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="showForm = false">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="submitting">{{ submitting ? '提交中...' : '提交投诉' }}</button>
          </div>
        </form>
      </div>

      <LoadingSpinner v-if="loading" />
      <EmptyState v-else-if="!complaints.length" icon="📝" title="暂无投诉" description="如遇到问题，可以提交投诉" />

      <div v-else class="complaint-list">
        <div v-for="c in complaints" :key="c.id" class="complaint-item card">
          <div class="complaint-header">
            <span class="complaint-title">{{ c.title }}</span>
            <span :class="['tag', statusClass(c.status)]">{{ statusText(c.status) }}</span>
          </div>
          <div class="complaint-meta">
            <span>类型：{{ typeText(c.type) }}</span>
            <span v-if="c.orderId">订单ID：{{ c.orderId }}</span>
            <span>{{ formatDate(c.createdAt) }}</span>
          </div>
          <p class="complaint-content">{{ c.content }}</p>
          <div v-if="c.adminResponse" class="complaint-response">
            <strong>处理结果：</strong>{{ c.adminResponse }}
            <span v-if="c.handledByName" class="handler">—— {{ c.handledByName }}</span>
            <span v-if="c.handledAt" class="handled-time">{{ formatDate(c.handledAt) }}</span>
          </div>
        </div>
      </div>

      <Pagination :current-page="page" :total-pages="totalPages" :total="total" @change="changePage" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { submitComplaint, getMyComplaints } from '@/api/complaint'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const complaints = ref([])
const page = ref(0)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)

const form = reactive({ type: 'OTHER', title: '', content: '', orderId: null, targetUserId: null })

function statusText(s) {
  const m = { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决' }
  return m[s] || s
}
function statusClass(s) {
  const m = { PENDING: 'tag-warning', PROCESSING: 'tag-info', RESOLVED: 'tag-success' }
  return m[s] || 'tag-default'
}
function typeText(t) {
  const m = { USER: '用户行为', PRODUCT: '商品问题', ORDER: '订单问题', OTHER: '其他' }
  return m[t] || t
}
function formatDate(d) { return d ? new Date(d).toLocaleDateString('zh-CN') : '-' }

async function fetchData() {
  loading.value = true
  try {
    const res = await getMyComplaints({ page: page.value, size: 10 })
    complaints.value = res.list || []
    total.value = res.total; totalPages.value = res.totalPages
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function changePage(p) { page.value = p; fetchData() }

async function handleSubmit() {
  if (!form.title.trim()) return window.$toast?.warning('请输入投诉标题')
  if (!form.content.trim()) return window.$toast?.warning('请输入投诉内容')
  submitting.value = true
  try {
    await submitComplaint({
      type: form.type, title: form.title, content: form.content,
      orderId: form.orderId || undefined, targetUserId: form.targetUserId || undefined
    })
    window.$toast?.success('投诉已提交')
    showForm.value = false
    form.title = ''; form.content = ''; form.orderId = null; form.targetUserId = null
    fetchData()
  } catch (err) { window.$toast?.error(err.message || '提交失败') }
  finally { submitting.value = false }
}

onMounted(fetchData)
</script>

<style scoped>
.page-header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 600; }

.complaint-form-card { padding: 28px; margin-bottom: 24px; }
.complaint-form-card h3 { font-size: 18px; font-weight: 600; margin-bottom: 18px; }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }

.complaint-list { display: flex; flex-direction: column; gap: 12px; }
.complaint-item { padding: 20px; }

.complaint-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.complaint-title { font-size: 16px; font-weight: 600; }
.complaint-meta { display: flex; gap: 16px; font-size: 13px; color: var(--text-muted); margin-bottom: 10px; }
.complaint-content { font-size: 14px; line-height: 1.6; color: var(--text-secondary); }

.complaint-response {
  margin-top: 12px; padding: 12px 16px;
  background: #E8F5E9; border-radius: var(--radius-sm);
  font-size: 14px; line-height: 1.5;
}
.complaint-response strong { color: #2E7D32; }
.handler { color: var(--text-muted); margin-left: 8px; }
.handled-time { color: var(--text-muted); margin-left: 8px; font-size: 12px; }
</style>
