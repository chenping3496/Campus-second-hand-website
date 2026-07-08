<template>
  <div class="admin-page">
    <div class="container">
      <div class="admin-layout">
        <AdminSidebar current="orders" />
        <div class="admin-content">
          <h2 class="admin-title">订单管理</h2>

          <div class="toolbar">
            <input v-model="orderNo" type="text" class="form-input toolbar-search" placeholder="搜索订单号..." @keyup.enter="fetchData" />
            <select v-model="statusFilter" class="form-input toolbar-select" @change="fetchData">
              <option value="">全部状态</option>
              <option value="PENDING">待处理</option>
              <option value="SHIPPED">已发货</option>
              <option value="COMPLETED">已完成</option>
              <option value="CANCELLED">已取消</option>
            </select>
          </div>

          <LoadingSpinner v-if="loading" />
          <EmptyState v-else-if="!orders.length" icon="📋" title="暂无订单" />

          <div v-else class="table-wrapper card">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th><th>订单号</th><th>商品</th><th>金额</th><th>买家</th><th>卖家</th><th>状态</th><th>时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="o in orders" :key="o.id">
                  <td>{{ o.id }}</td>
                  <td class="order-no-cell">{{ o.orderNo }}</td>
                  <td>{{ o.productTitle || '已删除' }}</td>
                  <td>¥{{ o.price }}</td>
                  <td>{{ o.buyerName || '-' }}</td>
                  <td>{{ o.sellerName || '-' }}</td>
                  <td><span :class="['tag', statusClass(o.status)]">{{ statusText(o.status) }}</span></td>
                  <td>{{ formatDate(o.createdAt) }}</td>
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
import { getAdminOrders } from '@/api/admin'
import AdminSidebar from '@/components/admin/AdminSidebar.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const orderNo = ref('')
const statusFilter = ref('')
const orders = ref([])
const page = ref(0)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)

function statusText(s) {
  const map = { PENDING: '待处理', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
  return map[s] || s
}

function statusClass(s) {
  const map = { PENDING: 'tag-warning', SHIPPED: 'tag-info', COMPLETED: 'tag-success', CANCELLED: 'tag-default' }
  return map[s] || 'tag-default'
}

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('zh-CN')
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminOrders({
      orderNo: orderNo.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value, size: 10
    })
    orders.value = res.list || []
    total.value = res.total || 0
    totalPages.value = res.totalPages || 0
  } catch (err) { window.$toast?.error(err.message || '加载失败') }
  finally { loading.value = false }
}

function changePage(p) { page.value = p; fetchData() }

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

.order-no-cell { max-width: 150px; overflow: hidden; text-overflow: ellipsis; font-family: monospace; font-size: 12px; }

@media (max-width: 768px) {
  .admin-layout { grid-template-columns: 1fr; }
}
</style>
