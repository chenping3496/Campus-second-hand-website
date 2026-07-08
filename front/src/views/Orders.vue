<template>
  <div class="orders-page">
    <div class="container">
      <h2 class="page-title">我的订单</h2>

      <div class="order-tabs">
        <button :class="['tab-btn', { active: tab === 'bought' }]" @click="switchTab('bought')">我买到的</button>
        <button :class="['tab-btn', { active: tab === 'sold' }]" @click="switchTab('sold')">我卖出的</button>
      </div>

      <div class="status-tabs">
        <button :class="['stab-btn', { active: !statusFilter }]" @click="changeStatus(null)">全部</button>
        <button :class="['stab-btn', { active: statusFilter === 'PENDING' }]" @click="changeStatus('PENDING')">待处理</button>
        <button :class="['stab-btn', { active: statusFilter === 'SHIPPED' }]" @click="changeStatus('SHIPPED')">已发货</button>
        <button :class="['stab-btn', { active: statusFilter === 'COMPLETED' }]" @click="changeStatus('COMPLETED')">已完成</button>
        <button :class="['stab-btn', { active: statusFilter === 'CANCELLED' }]" @click="changeStatus('CANCELLED')">已取消</button>
      </div>

      <LoadingSpinner v-if="loading" />
      <EmptyState v-else-if="!orders.length" icon="📋" title="暂无订单" description="还没有相关订单" />

      <div v-else class="order-list">
        <div v-for="order in orders" :key="order.id" class="order-item card">
          <div class="order-header">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span :class="['tag', statusClass(order.status)]">{{ statusText(order.status) }}</span>
          </div>
          <router-link :to="`/order/${order.id}`" class="order-body">
            <div class="order-image">
              <img v-if="order.productImage" :src="order.productImage" alt="" />
              <div v-else class="no-img">暂无图片</div>
            </div>
            <div class="order-info">
              <h4 class="order-title">{{ order.productTitle || '商品已删除' }}</h4>
              <div class="order-meta">
                <span class="order-price">¥{{ order.price }}</span>
                <span class="order-user">
                  {{ tab === 'bought' ? '卖家' : '买家' }}：
                  {{ tab === 'bought' ? order.sellerName : order.buyerName }}
                </span>
              </div>
              <span class="order-time">{{ formatDate(order.createdAt) }}</span>
            </div>
          </router-link>
          <div class="order-actions" v-if="order.status === 'PENDING' && tab === 'sold'">
            <button class="btn btn-primary btn-sm" @click="handleShip(order.id)">发货</button>
          </div>
          <div class="order-actions" v-if="order.status === 'SHIPPED' && tab === 'bought'">
            <button class="btn btn-primary btn-sm" @click="handleConfirm(order.id)">确认收货</button>
          </div>
          <div class="order-actions" v-if="order.status === 'PENDING'">
            <button class="btn btn-secondary btn-sm" @click="handleCancel(order.id)">取消订单</button>
          </div>
        </div>
      </div>

      <Pagination :current-page="page" :total-pages="totalPages" :total="total" @change="changePage" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBuyerOrders, getSellerOrders, shipOrder, confirmReceive, cancelOrder } from '@/api/order'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const tab = ref('bought')
const statusFilter = ref(null)
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
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN')
}

async function fetchData() {
  loading.value = true
  try {
    const fetcher = tab.value === 'bought' ? getBuyerOrders : getSellerOrders
    const res = await fetcher({ status: statusFilter.value || undefined, page: page.value, size: 10 })
    orders.value = res.list || []
    total.value = res.total || 0
    totalPages.value = res.totalPages || 0
  } catch (err) {
    window.$toast?.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function switchTab(t) { tab.value = t; statusFilter.value = null; page.value = 0; fetchData() }
function changeStatus(s) { statusFilter.value = s; page.value = 0; fetchData() }
function changePage(p) { page.value = p; fetchData() }

async function handleShip(id) {
  try { await shipOrder(id); window.$toast?.success('已发货'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}

async function handleConfirm(id) {
  if (!confirm('确认收货？')) return
  try { await confirmReceive(id); window.$toast?.success('已确认收货'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}

async function handleCancel(id) {
  const reason = prompt('取消原因（选填）：')
  try { await cancelOrder(id, reason || undefined); window.$toast?.success('已取消'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}

onMounted(fetchData)
</script>

<style scoped>
.page-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; }

.order-tabs { display: flex; gap: 0; margin-bottom: 12px; background: var(--bg-white); border-radius: var(--radius); padding: 4px; box-shadow: var(--shadow); }
.tab-btn { flex: 1; padding: 10px 0; background: none; border-radius: var(--radius-sm); font-size: 14px; color: var(--text-secondary); transition: all 0.2s; }
.tab-btn.active { background: var(--primary); color: #fff; }

.status-tabs { display: flex; gap: 8px; margin-bottom: 20px; overflow-x: auto; }
.stab-btn { padding: 6px 14px; background: var(--bg-white); border: 1px solid var(--border); border-radius: 20px; font-size: 13px; color: var(--text-secondary); white-space: nowrap; transition: all 0.2s; }
.stab-btn.active { background: var(--primary); color: #fff; border-color: var(--primary); }

.order-list { display: flex; flex-direction: column; gap: 12px; }

.order-item { padding: 0; overflow: hidden; }

.order-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 16px; background: var(--bg);
}

.order-no { font-size: 13px; color: var(--text-muted); }

.order-body {
  display: flex; gap: 14px; padding: 14px 16px; cursor: pointer; transition: background 0.2s;
}
.order-body:hover { background: var(--bg); }

.order-image { width: 80px; height: 80px; border-radius: var(--radius-sm); overflow: hidden; flex-shrink: 0; }
.order-image img { width: 100%; height: 100%; object-fit: cover; }
.no-img { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f0f0f0; color: var(--text-muted); font-size: 12px; }

.order-info { flex: 1; display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.order-title { font-size: 15px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.order-meta { display: flex; align-items: center; gap: 12px; }
.order-price { font-size: 17px; color: var(--danger); font-weight: 600; }
.order-user { font-size: 13px; color: var(--text-secondary); }
.order-time { font-size: 12px; color: var(--text-muted); }

.order-actions { display: flex; gap: 8px; padding: 10px 16px; border-top: 1px solid var(--border-light); justify-content: flex-end; }
</style>
