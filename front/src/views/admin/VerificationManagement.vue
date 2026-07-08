<template>
  <div class="admin-page">
    <div class="container">
      <div class="admin-layout">
        <AdminSidebar current="verifications" />
        <div class="admin-content">
          <h2 class="admin-title">认证管理</h2>

          <!-- Tabs: Verifications / Appeals -->
          <div class="tabs">
            <button :class="['tab', { active: tab === 'verifications' }]" @click="tab = 'verifications'; fetchData()">认证申请</button>
            <button :class="['tab', { active: tab === 'appeals' }]" @click="tab = 'appeals'; fetchData()">申诉处理</button>
          </div>

          <div class="toolbar">
            <select v-model="statusFilter" class="form-input toolbar-select" @change="fetchData">
              <option value="">全部状态</option>
              <option value="PENDING">待审核</option>
              <option value="APPROVED">已通过</option>
              <option value="REJECTED">已驳回</option>
              <option value="FROZEN">已冻结</option>
            </select>
          </div>

          <LoadingSpinner v-if="loading" />

          <!-- Verifications table -->
          <div v-else-if="tab === 'verifications' && items.length" class="table-wrapper card">
            <table class="data-table">
              <thead>
                <tr>
                  <th>用户</th><th>真实姓名</th><th>身份</th><th>学号/工号</th><th>证件照</th><th>状态</th><th>备注</th><th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="v in items" :key="v.userId">
                  <td class="user-cell">
                    <img v-if="v.avatar" :src="v.avatar" class="user-thumb" />
                    <div>
                      <span class="user-nick">{{ v.nickname || v.username }}</span>
                      <span class="user-uid">ID:{{ v.userId }}</span>
                    </div>
                  </td>
                  <td>{{ v.realName || '-' }}</td>
                  <td><span :class="['tag', v.identityType === 'TEACHER' ? 'tag-info' : 'tag-primary']">{{ v.identityType === 'TEACHER' ? '教师' : '学生' }}</span></td>
                  <td>{{ v.identityNumber || '-' }}</td>
                  <td>
                    <a v-if="v.idCardImage" :href="v.idCardImage" target="_blank" class="view-link">查看证件</a>
                    <span v-else>-</span>
                  </td>
                  <td><span :class="['tag', statusClass(v.verificationStatus)]">{{ statusText(v.verificationStatus) }}</span></td>
                  <td class="remark-cell">{{ v.verificationRemark || '-' }}</td>
                  <td class="actions-cell">
                    <button v-if="v.verificationStatus === 'PENDING'" class="btn btn-primary btn-sm" @click="handleApprove(v)">通过</button>
                    <button v-if="v.verificationStatus === 'PENDING'" class="btn btn-danger btn-sm" @click="handleReject(v)">驳回</button>
                    <button v-if="v.verificationStatus === 'APPROVED'" class="btn btn-warning btn-sm" @click="handleFreeze(v)">冻结</button>
                    <button v-if="v.verificationStatus === 'FROZEN'" class="btn btn-primary btn-sm" @click="handleUnfreeze(v)">解冻</button>
                    <button v-if="v.verificationStatus === 'REJECTED' || v.verificationStatus === 'FROZEN'" class="btn btn-outline btn-sm" @click="handleReview(v)">复核</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Appeals table -->
          <div v-else-if="tab === 'appeals' && items.length" class="table-wrapper card">
            <table class="data-table">
              <thead>
                <tr>
                  <th>用户</th><th>申诉原因</th><th>补充材料</th><th>状态</th><th>时间</th><th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="a in items" :key="a.id">
                  <td class="user-cell">
                    <span class="user-nick">{{ a.nickname || a.username }}</span>
                    <span class="user-uid">ID:{{ a.userId }}</span>
                  </td>
                  <td class="reason-cell">{{ a.reason }}</td>
                  <td>
                    <a v-if="a.image" :href="a.image" target="_blank" class="view-link">查看材料</a>
                    <span v-else>-</span>
                  </td>
                  <td><span :class="['tag', appealStatusClass(a.status)]">{{ appealStatusText(a.status) }}</span></td>
                  <td>{{ formatDate(a.createdAt) }}</td>
                  <td class="actions-cell" v-if="a.status === 'PENDING'">
                    <button class="btn btn-primary btn-sm" @click="handleAppealApprove(a)">通过</button>
                    <button class="btn btn-danger btn-sm" @click="handleAppealReject(a)">驳回</button>
                  </td>
                  <td v-else>
                    <span class="text-muted">{{ a.adminResponse || '已处理' }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <EmptyState v-else icon="📋" title="暂无数据" />

          <Pagination :current-page="page" :total-pages="totalPages" :total="total" @change="changePage" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  getVerificationList, approveVerification, rejectVerification,
  freezeVerification, unfreezeVerification, reviewVerification,
  getAppeals, handleAppeal
} from '@/api/verification'
import AdminSidebar from '@/components/admin/AdminSidebar.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const tab = ref('verifications')
const statusFilter = ref('')
const items = ref([])
const page = ref(0)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)

function statusText(s) {
  const map = { UNVERIFIED: '未认证', PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', FROZEN: '已冻结' }
  return map[s] || s
}
function statusClass(s) {
  const map = { UNVERIFIED: 'tag-default', PENDING: 'tag-warning', APPROVED: 'tag-success', REJECTED: 'tag-danger', FROZEN: 'tag-info' }
  return map[s] || 'tag-default'
}
function appealStatusText(s) {
  const map = { PENDING: '待处理', APPROVED: '已通过', REJECTED: '已驳回' }
  return map[s] || s
}
function appealStatusClass(s) {
  const map = { PENDING: 'tag-warning', APPROVED: 'tag-success', REJECTED: 'tag-danger' }
  return map[s] || 'tag-default'
}
function formatDate(d) { return d ? new Date(d).toLocaleDateString('zh-CN') : '-' }

async function fetchData() {
  loading.value = true
  try {
    if (tab.value === 'verifications') {
      const res = await getVerificationList({ status: statusFilter.value || undefined, page: page.value, size: 10 })
      items.value = res.list || []
      total.value = res.total; totalPages.value = res.totalPages
    } else {
      const res = await getAppeals({ status: statusFilter.value || undefined, page: page.value, size: 10 })
      items.value = res.list || []
      total.value = res.total; totalPages.value = res.totalPages
    }
  } catch (err) { window.$toast?.error('加载失败') }
  finally { loading.value = false }
}

function changePage(p) { page.value = p; fetchData() }

async function handleApprove(v) {
  try { await approveVerification(v.userId); window.$toast?.success('已通过'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}
async function handleReject(v) {
  const reason = prompt('驳回原因：')
  if (reason === null) return
  try { await rejectVerification(v.userId, reason); window.$toast?.success('已驳回'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}
async function handleFreeze(v) {
  if (!confirm('确定冻结该用户的认证？')) return
  try { await freezeVerification(v.userId); window.$toast?.success('已冻结'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}
async function handleUnfreeze(v) {
  try { await unfreezeVerification(v.userId); window.$toast?.success('已解冻'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}
async function handleReview(v) {
  try { await reviewVerification(v.userId); window.$toast?.success('已设为复核中'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}
async function handleAppealApprove(a) {
  const resp = prompt('回复（选填）：')
  if (resp === null) return
  try { await handleAppeal(a.id, { approved: true, response: resp || undefined }); window.$toast?.success('申诉已通过'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}
async function handleAppealReject(a) {
  const resp = prompt('驳回原因：')
  if (resp === null) return
  try { await handleAppeal(a.id, { approved: false, response: resp }); window.$toast?.success('申诉已驳回'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}

onMounted(fetchData)
</script>

<style scoped>
.admin-page { padding: 24px 0 40px; }
.admin-layout { display: grid; grid-template-columns: 220px 1fr; gap: 24px; }
.admin-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; }

.tabs { display: flex; gap: 0; margin-bottom: 16px; background: var(--bg-white); border-radius: var(--radius); padding: 4px; box-shadow: var(--shadow); }
.tab { flex: 1; padding: 10px 0; background: none; border-radius: var(--radius-sm); font-size: 14px; color: var(--text-secondary); }
.tab.active { background: var(--primary); color: #fff; }

.toolbar { margin-bottom: 16px; }
.toolbar-select { max-width: 160px; }

.table-wrapper { padding: 0; overflow-x: auto; }

.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th, .data-table td { padding: 12px 14px; text-align: left; border-bottom: 1px solid var(--border-light); white-space: nowrap; }
.data-table th { background: var(--bg); font-weight: 600; color: var(--text-secondary); }
.data-table tr:hover td { background: #fafafa; }

.user-cell { display: flex; flex-direction: column; gap: 2px; }
.user-thumb { width: 28px; height: 28px; border-radius: 50%; }
.user-nick { font-weight: 500; }
.user-uid { font-size: 11px; color: var(--text-muted); }

.view-link { color: var(--info); font-size: 13px; }
.view-link:hover { text-decoration: underline; }

.remark-cell { max-width: 150px; overflow: hidden; text-overflow: ellipsis; }
.reason-cell { max-width: 200px; overflow: hidden; text-overflow: ellipsis; }
.actions-cell { display: flex; gap: 6px; }

.btn-warning { background: var(--warning); color: #fff; }
.btn-warning:hover { background: #E65100; }

.text-muted { font-size: 13px; color: var(--text-muted); }

@media (max-width: 768px) {
  .admin-layout { grid-template-columns: 1fr; }
}
</style>
