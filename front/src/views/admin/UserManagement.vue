<template>
  <div class="admin-page">
    <div class="container">
      <div class="admin-layout">
        <AdminSidebar current="users" />
        <div class="admin-content">
          <h2 class="admin-title">用户管理</h2>

          <div class="toolbar">
            <input v-model="keyword" type="text" class="form-input toolbar-search" placeholder="搜索用户名..." @keyup.enter="fetchData" />
            <select v-model="statusFilter" class="form-input toolbar-select" @change="fetchData">
              <option value="">全部状态</option>
              <option value="ACTIVE">正常</option>
              <option value="BANNED">已封禁</option>
            </select>
          </div>

          <LoadingSpinner v-if="loading" />
          <EmptyState v-else-if="!users.length" icon="👥" title="暂无用户" />

          <div v-else class="table-wrapper card">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th><th>用户名</th><th>昵称</th><th>手机</th><th>邮箱</th><th>角色</th><th>状态</th><th>注册时间</th><th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="u in users" :key="u.id">
                  <td>{{ u.id }}</td>
                  <td>{{ u.username }}</td>
                  <td>{{ u.nickname || '-' }}</td>
                  <td>{{ u.phone || '-' }}</td>
                  <td>{{ u.email || '-' }}</td>
                  <td><span :class="['tag', u.role === 'ADMIN' ? 'tag-warning' : 'tag-primary']">{{ u.role === 'ADMIN' ? '管理员' : '用户' }}</span></td>
                  <td><span :class="['tag', u.status === 'ACTIVE' ? 'tag-success' : 'tag-danger']">{{ u.status === 'ACTIVE' ? '正常' : '已封禁' }}</span></td>
                  <td>{{ formatDate(u.createdAt) }}</td>
                  <td>
                    <button v-if="u.status === 'ACTIVE' && u.role !== 'ADMIN'" class="btn btn-danger btn-sm" @click="handleBan(u)">封禁</button>
                    <button v-if="u.status === 'BANNED'" class="btn btn-primary btn-sm" @click="handleUnban(u)">解禁</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <Pagination :current-page="page" :total-pages="totalPages" :total="total" @change="changePage" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminUsers, banUser, unbanUser } from '@/api/admin'
import AdminSidebar from '@/components/admin/AdminSidebar.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const keyword = ref('')
const statusFilter = ref('')
const users = ref([])
const page = ref(0)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('zh-CN')
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminUsers({
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value, size: 10
    })
    users.value = res.list || []
    total.value = res.total || 0
    totalPages.value = res.totalPages || 0
  } catch (err) { window.$toast?.error(err.message || '加载失败') }
  finally { loading.value = false }
}

function changePage(p) { page.value = p; fetchData() }

async function handleBan(u) {
  if (!confirm(`确定要封禁用户「${u.username}」吗？`)) return
  try { await banUser(u.id); window.$toast?.success('已封禁'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}

async function handleUnban(u) {
  try { await unbanUser(u.id); window.$toast?.success('已解禁'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}

onMounted(fetchData)
</script>

<style scoped>
.admin-page { padding: 24px 0 40px; }
.admin-layout { display: grid; grid-template-columns: 220px 1fr; gap: 24px; }
.admin-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; }

.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
.toolbar-search { max-width: 260px; }
.toolbar-select { max-width: 140px; }

.table-wrapper { padding: 0; overflow-x: auto; }

.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th, .data-table td { padding: 12px 14px; text-align: left; border-bottom: 1px solid var(--border-light); white-space: nowrap; }
.data-table th { background: var(--bg); font-weight: 600; color: var(--text-secondary); }
.data-table tr:hover td { background: #fafafa; }

@media (max-width: 768px) {
  .admin-layout { grid-template-columns: 1fr; }
}
</style>
