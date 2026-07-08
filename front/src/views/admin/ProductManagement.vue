<template>
  <div class="admin-page">
    <div class="container">
      <div class="admin-layout">
        <AdminSidebar current="products" />
        <div class="admin-content">
          <h2 class="admin-title">商品管理</h2>

          <div class="toolbar">
            <input v-model="keyword" type="text" class="form-input toolbar-search" placeholder="搜索商品..." @keyup.enter="fetchData" />
            <select v-model="statusFilter" class="form-input toolbar-select" @change="fetchData">
              <option value="">全部状态</option>
              <option value="PENDING">待审核</option>
              <option value="ON_SALE">在售</option>
              <option value="SOLD">已售出</option>
              <option value="OFF_SHELF">已下架</option>
              <option value="REJECTED">已拒绝</option>
            </select>
          </div>

          <LoadingSpinner v-if="loading" />
          <EmptyState v-else-if="!products.length" icon="📦" title="暂无商品" />

          <div v-else class="table-wrapper card">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th><th>图片</th><th>标题</th><th>价格</th><th>卖家</th><th>分类</th><th>状态</th><th>时间</th><th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in products" :key="p.id">
                  <td>{{ p.id }}</td>
                  <td>
                    <img v-if="p.images && p.images.length" :src="p.images[0]" class="product-thumb" />
                    <span v-else>-</span>
                  </td>
                  <td class="title-cell">{{ p.title }}</td>
                  <td>¥{{ p.price }}</td>
                  <td>{{ p.sellerName || '-' }}</td>
                  <td>{{ p.categoryName || '-' }}</td>
                  <td><span :class="['tag', statusClass(p.status)]">{{ statusText(p.status) }}</span></td>
                  <td>{{ formatDate(p.createdAt) }}</td>
                  <td class="actions-cell">
                    <button v-if="p.status === 'PENDING'" class="btn btn-primary btn-sm" @click="handleApprove(p)">通过</button>
                    <button v-if="p.status === 'PENDING'" class="btn btn-danger btn-sm" @click="handleReject(p)">拒绝</button>
                    <button v-if="p.status === 'ON_SALE'" class="btn btn-danger btn-sm" @click="handleForceOff(p)">强制下架</button>
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
import { getAdminProducts, approveProduct, rejectProduct, forceOffShelf } from '@/api/admin'
import AdminSidebar from '@/components/admin/AdminSidebar.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const keyword = ref('')
const statusFilter = ref('PENDING')
const products = ref([])
const page = ref(0)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)

function statusText(s) {
  const map = { ON_SALE: '在售', SOLD: '已售出', OFF_SHELF: '已下架', PENDING: '待审核', REJECTED: '已拒绝' }
  return map[s] || s
}

function statusClass(s) {
  const map = { ON_SALE: 'tag-success', SOLD: 'tag-default', OFF_SHELF: 'tag-warning', PENDING: 'tag-info', REJECTED: 'tag-danger' }
  return map[s] || 'tag-default'
}

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('zh-CN')
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminProducts({
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value, size: 10
    })
    products.value = res.list || []
    total.value = res.total || 0
    totalPages.value = res.totalPages || 0
  } catch (err) { window.$toast?.error(err.message || '加载失败') }
  finally { loading.value = false }
}

function changePage(p) { page.value = p; fetchData() }

async function handleApprove(p) {
  try { await approveProduct(p.id); window.$toast?.success('已通过审核'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}

async function handleReject(p) {
  const reason = prompt('拒绝原因：')
  if (reason === null) return
  try { await rejectProduct(p.id, reason); window.$toast?.success('已拒绝'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}

async function handleForceOff(p) {
  const reason = prompt('下架原因：')
  if (reason === null) return
  try { await forceOffShelf(p.id, reason); window.$toast?.success('已强制下架'); fetchData() }
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

.product-thumb { width: 40px; height: 40px; border-radius: 4px; object-fit: cover; }
.title-cell { max-width: 200px; overflow: hidden; text-overflow: ellipsis; }
.actions-cell { display: flex; gap: 6px; }

@media (max-width: 768px) {
  .admin-layout { grid-template-columns: 1fr; }
}
</style>
