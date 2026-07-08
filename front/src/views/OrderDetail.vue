<template>
  <div class="order-detail-page">
    <div class="container">
      <router-link to="/orders" class="back-link">← 返回订单列表</router-link>
      <LoadingSpinner v-if="loading" />
      <div v-else-if="order" class="card order-card">
        <div class="order-status-bar" :class="statusClass(order.status)">
          <span class="status-icon">{{ statusIcon(order.status) }}</span>
          <span class="status-text">{{ statusText(order.status) }}</span>
        </div>

        <div class="order-section">
          <h3>订单信息</h3>
          <div class="info-grid">
            <div><span class="label">订单编号</span><span>{{ order.orderNo }}</span></div>
            <div><span class="label">下单时间</span><span>{{ formatDate(order.createdAt) }}</span></div>
            <div><span class="label">支付方式</span><span>{{ order.paymentMethod || '在线支付' }}</span></div>
            <div v-if="order.paymentTime"><span class="label">支付时间</span><span>{{ formatDate(order.paymentTime) }}</span></div>
            <div v-if="order.shipTime"><span class="label">发货时间</span><span>{{ formatDate(order.shipTime) }}</span></div>
            <div v-if="order.completeTime"><span class="label">完成时间</span><span>{{ formatDate(order.completeTime) }}</span></div>
            <div v-if="order.cancelTime"><span class="label">取消时间</span><span>{{ formatDate(order.cancelTime) }}</span></div>
            <div v-if="order.cancelReason"><span class="label">取消原因</span><span>{{ order.cancelReason }}</span></div>
          </div>
        </div>

        <div class="order-section">
          <h3>商品信息</h3>
          <router-link v-if="order.productId" :to="`/product/${order.productId}`" class="product-link">
            <img v-if="order.productImage" :src="order.productImage" class="product-img" />
            <div v-else class="no-img">暂无图片</div>
            <div>
              <p class="product-name">{{ order.productTitle || '商品已删除' }}</p>
              <p class="product-price">¥{{ order.price }}</p>
            </div>
          </router-link>
        </div>

        <div class="order-section">
          <h3>交易对方</h3>
          <div class="user-info">
            <img v-if="otherUser.avatar" :src="otherUser.avatar" class="user-avatar" />
            <span v-else class="avatar-placeholder">👤</span>
            <span>{{ otherUser.name }}</span>
          </div>
        </div>

        <div class="order-actions-bottom" v-if="order.status === 'PENDING' && isSeller">
          <button class="btn btn-primary" @click="handleShip">发货</button>
          <button class="btn btn-secondary" @click="handleCancel">取消</button>
        </div>
        <div class="order-actions-bottom" v-if="order.status === 'SHIPPED' && isBuyer">
          <button class="btn btn-primary" @click="handleConfirm">确认收货</button>
        </div>
        <div class="order-actions-bottom" v-if="order.status === 'PENDING' && isBuyer">
          <button class="btn btn-secondary" @click="handleCancel">取消订单</button>
        </div>
      </div>
      <EmptyState v-else icon="😕" title="订单不存在" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, shipOrder, confirmReceive, cancelOrder } from '@/api/order'
import { useAuthStore } from '@/stores/auth'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const order = ref(null)
const loading = ref(true)

const isBuyer = computed(() => auth.user && order.value && auth.user.id === order.value.buyerId)
const isSeller = computed(() => auth.user && order.value && auth.user.id === order.value.sellerId)

const otherUser = computed(() => {
  if (!order.value) return {}
  if (isBuyer.value) return { name: order.value.sellerName, avatar: order.value.sellerAvatar }
  return { name: order.value.buyerName, avatar: order.value.buyerAvatar }
})

function statusText(s) {
  const map = { PENDING: '待处理', SHIPPED: '卖家已发货', COMPLETED: '交易完成', CANCELLED: '已取消' }
  return map[s] || s
}

function statusClass(s) {
  const map = { PENDING: 'status-pending', SHIPPED: 'status-shipped', COMPLETED: 'status-completed', CANCELLED: 'status-cancelled' }
  return map[s] || ''
}

function statusIcon(s) {
  const map = { PENDING: '⏳', SHIPPED: '📦', COMPLETED: '✅', CANCELLED: '❌' }
  return map[s] || '📋'
}

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleString('zh-CN')
}

async function fetchData() {
  loading.value = true
  try { order.value = await getOrderDetail(route.params.id) }
  catch { order.value = null }
  finally { loading.value = false }
}

async function handleShip() {
  try { await shipOrder(order.value.id); window.$toast?.success('已发货'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}

async function handleConfirm() {
  if (!confirm('确认收货？')) return
  try { await confirmReceive(order.value.id); window.$toast?.success('已确认收货'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}

async function handleCancel() {
  const reason = prompt('取消原因（选填）：')
  try { await cancelOrder(order.value.id, reason || undefined); window.$toast?.success('已取消'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '操作失败') }
}

onMounted(fetchData)
</script>

<style scoped>
.back-link { display: inline-block; margin-bottom: 16px; font-size: 14px; color: var(--text-secondary); }
.back-link:hover { color: var(--primary); }

.order-card { max-width: 700px; margin: 0 auto; }

.order-status-bar {
  display: flex; align-items: center; gap: 10px;
  padding: 16px 20px; border-radius: var(--radius); margin-bottom: 20px;
  font-size: 18px; font-weight: 600;
}

.status-pending { background: #FFF3E0; color: #E65100; }
.status-shipped { background: #E3F2FD; color: #1565C0; }
.status-completed { background: #E8F5E9; color: #2E7D32; }
.status-cancelled { background: #F5F5F5; color: #666; }

.order-section {
  margin-bottom: 20px; padding-top: 16px; border-top: 1px solid var(--border-light);
}

.order-section h3 { font-size: 16px; font-weight: 600; margin-bottom: 12px; }

.info-grid { display: flex; flex-direction: column; gap: 8px; }
.info-grid > div { display: flex; font-size: 14px; }
.label { color: var(--text-muted); width: 80px; flex-shrink: 0; }

.product-link { display: flex; gap: 12px; align-items: center; }
.product-img { width: 60px; height: 60px; border-radius: var(--radius-sm); object-fit: cover; }
.no-img { width: 60px; height: 60px; border-radius: var(--radius-sm); background: #f0f0f0; display: flex; align-items: center; justify-content: center; color: var(--text-muted); font-size: 12px; }
.product-name { font-size: 14px; }
.product-price { font-size: 16px; color: var(--danger); font-weight: 600; }

.user-info { display: flex; align-items: center; gap: 10px; }
.user-avatar { width: 36px; height: 36px; border-radius: 50%; }
.avatar-placeholder { font-size: 24px; }

.order-actions-bottom { display: flex; gap: 10px; margin-top: 20px; }
</style>
