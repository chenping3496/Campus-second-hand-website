<template>
  <div class="admin-page">
    <div class="container">
      <div class="admin-layout">
        <AdminSidebar current="complaints" />
        <div class="admin-content">
          <h2 class="admin-title">投诉管理</h2>

          <div class="toolbar">
            <select v-model="statusFilter" class="form-input toolbar-select" @change="fetchData">
              <option value="">全部状态</option>
              <option value="PENDING">待处理</option>
              <option value="PROCESSING">处理中</option>
              <option value="RESOLVED">已解决</option>
            </select>
            <select v-model="typeFilter" class="form-input toolbar-select" @change="fetchData">
              <option value="">全部类型</option>
              <option value="USER">用户行为</option>
              <option value="PRODUCT">商品问题</option>
              <option value="ORDER">订单问题</option>
              <option value="OTHER">其他</option>
            </select>
          </div>

          <LoadingSpinner v-if="loading" />
          <EmptyState v-else-if="!complaints.length" icon="📝" title="暂无投诉" />

          <div v-else class="table-wrapper card">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th><th>投诉人</th><th>类型</th><th>标题</th><th>被投诉</th><th>状态</th><th>时间</th><th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in complaints" :key="c.id" @click="showDetail(c)" style="cursor:pointer">
                  <td>{{ c.id }}</td>
                  <td>{{ c.userNickname || c.username }}</td>
                  <td><span :class="['tag', typeClass(c.type)]">{{ typeText(c.type) }}</span></td>
                  <td class="title-cell">{{ c.title }}</td>
                  <td>{{ c.targetNickname || c.targetUsername || '-' }}</td>
                  <td><span :class="['tag', statusClass(c.status)]">{{ statusText(c.status) }}</span></td>
                  <td>{{ formatDate(c.createdAt) }}</td>
                  <td class="actions-cell" @click.stop>
                    <button v-if="c.status === 'PENDING'" class="btn btn-info btn-sm" @click="handleProcessing(c)">受理</button>
                    <button v-if="c.status !== 'RESOLVED'" class="btn btn-primary btn-sm" @click="resolveDialog(c)">解决</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <Pagination :current-page="page" :total-pages="totalPages" :total="total" @change="changePage" />

          <!-- Detail dialog -->
          <div v-if="selected" class="dialog-overlay" @click.self="selected = null">
            <div class="dialog-card detail-dialog">
              <h3>投诉详情 #{{ selected.id }}</h3>
              <div class="detail-grid">
                <div><span class="dl">投诉人</span><span>{{ selected.userNickname || selected.username }}</span></div>
                <div><span class="dl">类型</span><span>{{ typeText(selected.type) }}</span></div>
                <div><span class="dl">标题</span><span>{{ selected.title }}</span></div>
                <div v-if="selected.orderId"><span class="dl">关联订单</span><span>#{{ selected.orderId }}</span></div>
                <div v-if="selected.targetUserId"><span class="dl">被投诉人</span><span>{{ selected.targetNickname || selected.targetUsername }}</span></div>
                <div><span class="dl">状态</span><span :class="['tag', statusClass(selected.status)]">{{ statusText(selected.status) }}</span></div>
                <div><span class="dl">时间</span><span>{{ formatDate(selected.createdAt) }}</span></div>
              </div>
              <div class="detail-content">
                <strong>投诉内容：</strong>
                <p>{{ selected.content }}</p>
              </div>
              <div v-if="selected.adminResponse" class="detail-response">
                <strong>处理结果：</strong>{{ selected.adminResponse }}
                <p class="handler-info" v-if="selected.handledByName">处理人：{{ selected.handledByName }} | {{ formatDate(selected.handledAt) }}</p>
              </div>
              <div class="dialog-actions">
                <button v-if="selected.status === 'PENDING'" class="btn btn-info" @click="handleProcessing(selected)">标记受理</button>
                <button v-if="selected.status !== 'RESOLVED'" class="btn btn-primary" @click="resolveDialog(selected)">解决投诉</button>
                <button class="btn btn-secondary" @click="selected = null">关闭</button>
              </div>
            </div>
          </div>

          <!-- Resolve dialog -->
          <div v-if="resolveTarget" class="dialog-overlay" @click.self="resolveTarget = null">
            <div class="dialog-card">
              <h3>解决投诉 #{{ resolveTarget.id }}</h3>
              <div class="form-group">
                <label>处理结果 *</label>
                <textarea v-model="resolveResponse" class="form-textarea" rows="4" placeholder="请输入处理结果说明..."></textarea>
              </div>
              <div class="dialog-actions">
                <button class="btn btn-secondary" @click="resolveTarget = null">取消</button>
                <button class="btn btn-primary" @click="handleResolve" :disabled="!resolveResponse.trim()">确认解决</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getComplaintList, markProcessing, resolveComplaint } from '@/api/complaint'
import AdminSidebar from '@/components/admin/AdminSidebar.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const statusFilter = ref('')
const typeFilter = ref('')
const complaints = ref([])
const page = ref(0)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)

const selected = ref(null)
const resolveTarget = ref(null)
const resolveResponse = ref('')

function statusText(s) { const m = { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决' }; return m[s] || s }
function statusClass(s) { const m = { PENDING: 'tag-warning', PROCESSING: 'tag-info', RESOLVED: 'tag-success' }; return m[s] || 'tag-default' }
function typeText(t) { const m = { USER: '用户行为', PRODUCT: '商品问题', ORDER: '订单问题', OTHER: '其他' }; return m[t] || t }
function typeClass(t) { const m = { USER: 'tag-danger', PRODUCT: 'tag-warning', ORDER: 'tag-info', OTHER: 'tag-default' }; return m[t] || 'tag-default' }
function formatDate(d) { return d ? new Date(d).toLocaleDateString('zh-CN') : '-' }

async function fetchData() {
  loading.value = true
  try {
    const res = await getComplaintList({ status: statusFilter.value || undefined, type: typeFilter.value || undefined, page: page.value, size: 10 })
    complaints.value = res.list || []
    total.value = res.total; totalPages.value = res.totalPages
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function changePage(p) { page.value = p; fetchData() }
function showDetail(c) { selected.value = c }
function resolveDialog(c) { resolveTarget.value = c; resolveResponse.value = '' }

async function handleProcessing(c) {
  try { await markProcessing(c.id); window.$toast?.success('已标记处理中'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}

async function handleResolve() {
  if (!resolveResponse.value.trim()) return
  try {
    await resolveComplaint(resolveTarget.value.id, resolveResponse.value)
    window.$toast?.success('投诉已解决')
    resolveTarget.value = null; selected.value = null
    fetchData()
  } catch (err) { window.$toast?.error(err.message || '操作失败') }
}

onMounted(fetchData)
</script>

<style scoped>
.admin-page { padding: 24px 0 40px; }
.admin-layout { display: grid; grid-template-columns: 220px 1fr; gap: 24px; }
.admin-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; }

.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
.toolbar-select { max-width: 150px; }

.table-wrapper { padding: 0; overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th, .data-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--border-light); white-space: nowrap; }
.data-table th { background: var(--bg); font-weight: 600; color: var(--text-secondary); }
.data-table tr:hover td { background: #fafafa; }
.title-cell { max-width: 200px; overflow: hidden; text-overflow: ellipsis; }
.actions-cell { display: flex; gap: 6px; }

.btn-info { background: var(--info); color: #fff; }
.btn-info:hover { background: #1565C0; }

/* Dialogs */
.dialog-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 9999; }
.dialog-card { background: var(--bg-white); border-radius: var(--radius-lg); padding: 28px; max-width: 560px; width: 90%; box-shadow: 0 8px 32px rgba(0,0,0,0.2); max-height: 80vh; overflow-y: auto; }
.dialog-card h3 { font-size: 18px; font-weight: 600; margin-bottom: 18px; }

.detail-grid { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; font-size: 14px; }
.detail-grid > div { display: flex; }
.dl { color: var(--text-muted); width: 80px; flex-shrink: 0; }

.detail-content { margin-bottom: 16px; }
.detail-content p { margin-top: 6px; font-size: 14px; line-height: 1.6; color: var(--text-secondary); white-space: pre-wrap; }
.detail-response { padding: 12px; background: #E8F5E9; border-radius: var(--radius-sm); font-size: 14px; }
.handler-info { font-size: 12px; color: var(--text-muted); margin-top: 4px; }

.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }

@media (max-width: 768px) { .admin-layout { grid-template-columns: 1fr; } }
</style>
