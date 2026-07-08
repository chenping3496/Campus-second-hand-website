<template>
  <div class="product-detail-page">
    <LoadingSpinner v-if="loading" text="加载商品详情..." />
    <div v-else-if="product" class="container">
      <!-- Breadcrumb -->
      <div class="breadcrumb">
        <router-link to="/">首页</router-link>
        <span> / </span>
        <router-link v-if="product.categoryId" :to="`/?categoryId=${product.categoryId}`">{{ product.categoryName }}</router-link>
        <span v-else>商品详情</span>
      </div>

      <div class="detail-main">
        <!-- Images -->
        <div class="detail-images">
          <div class="main-image">
            <img
              v-if="product.images && product.images.length"
              :src="product.images[selectedImage]"
              :alt="product.title"
            />
            <div v-else class="no-image">暂无图片</div>
          </div>
          <div class="thumb-list" v-if="product.images && product.images.length > 1">
            <div
              v-for="(img, i) in product.images"
              :key="i"
              :class="['thumb', { active: i === selectedImage }]"
              @click="selectedImage = i"
            >
              <img :src="img" alt="" />
            </div>
          </div>
        </div>

        <!-- Info -->
        <div class="detail-info">
          <h1 class="info-title">{{ product.title }}</h1>
          <div class="info-price-row">
            <span class="info-price"><span class="price-unit">¥</span>{{ product.price }}</span>
            <span v-if="product.originalPrice" class="price-original">¥{{ product.originalPrice }}</span>
            <span :class="['tag', statusTagClass]">{{ statusText }}</span>
          </div>

          <div class="info-meta">
            <div class="meta-item">
              <span class="meta-label">分类</span>
              <span class="meta-value">{{ product.categoryName || '未分类' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">浏览</span>
              <span class="meta-value">{{ product.viewCount || 0 }} 次</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">发布时间</span>
              <span class="meta-value">{{ formatDate(product.createdAt) }}</span>
            </div>
          </div>

          <!-- Seller -->
          <div class="seller-card">
            <router-link :to="`/user/${product.sellerId}`" class="seller-header">
              <img v-if="product.sellerAvatar" :src="product.sellerAvatar" class="seller-avatar" />
              <span v-else class="seller-avatar-placeholder">👤</span>
              <span class="seller-name">{{ product.sellerName || '匿名用户' }}</span>
            </router-link>
          </div>

          <!-- Actions -->
          <div class="info-actions" v-if="isSeller">
            <router-link :to="`/edit-product/${product.id}`" class="btn btn-outline">编辑商品</router-link>
            <button v-if="product.status === 'ON_SALE'" class="btn btn-secondary" @click="handleOffShelf">下架商品</button>
            <button v-if="product.status === 'OFF_SHELF'" class="btn btn-primary" @click="handleRelist">重新上架</button>
            <button class="btn btn-danger" @click="handleDelete">删除</button>
          </div>
          <div class="info-actions" v-else-if="product.status === 'ON_SALE'">
            <button
              :class="['btn', isFavorited ? 'btn-secondary' : 'btn-outline']"
              @click="toggleFavorite"
            >
              {{ isFavorited ? '❤️ 已收藏' : '🤍 收藏' }}
            </button>
            <button class="btn btn-outline" @click="handleChat">💬 联系卖家</button>
            <button class="btn btn-primary" @click="showBuyDialog = true">立即购买</button>
          </div>
        </div>
      </div>

      <!-- Description -->
      <div class="detail-section card">
        <h2 class="section-title">商品描述</h2>
        <p class="description-text">{{ product.description || '卖家没有留下描述...' }}</p>
      </div>
    </div>

    <!-- Error state -->
    <EmptyState v-else icon="😕" title="商品不存在" description="该商品可能已被删除或下架">
      <router-link to="/" class="btn btn-primary" style="margin-top:16px">返回首页</router-link>
    </EmptyState>

    <!-- Buy dialog -->
    <ConfirmDialog
      :visible="showBuyDialog"
      title="确认购买"
      :message="`确定要购买「${product?.title}」吗？价格：¥${product?.price}`"
      confirm-text="确认购买"
      @confirm="handleBuy"
      @cancel="showBuyDialog = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, offShelfProduct, relistProduct, deleteProduct } from '@/api/product'
import { checkFavorite, addFavorite, removeFavorite } from '@/api/favorite'
import { createOrder } from '@/api/order'
import { getOrCreateConversation } from '@/api/chat'
import { useAuthStore } from '@/stores/auth'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const product = ref(null)
const loading = ref(true)
const selectedImage = ref(0)
const isFavorited = ref(false)
const showBuyDialog = ref(false)

const isSeller = computed(() => {
  return auth.user && product.value && auth.user.id === product.value.sellerId
})

const statusText = computed(() => {
  const map = { ON_SALE: '在售', SOLD: '已售出', OFF_SHELF: '已下架', PENDING: '审核中', REJECTED: '已拒绝' }
  return map[product.value?.status] || product.value?.status || ''
})

const statusTagClass = computed(() => {
  const map = { ON_SALE: 'tag-success', SOLD: 'tag-default', OFF_SHELF: 'tag-warning', PENDING: 'tag-info', REJECTED: 'tag-danger' }
  return map[product.value?.status] || 'tag-default'
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN')
}

async function fetchDetail() {
  loading.value = true
  try {
    product.value = await getProductDetail(route.params.id)
    if (auth.isLoggedIn && !isSeller.value) {
      try {
        isFavorited.value = await checkFavorite(route.params.id)
      } catch { /* ignore */ }
    }
  } catch {
    product.value = null
  } finally {
    loading.value = false
  }
}

async function toggleFavorite() {
  if (!auth.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    if (isFavorited.value) {
      await removeFavorite(product.value.id)
      isFavorited.value = false
      window.$toast?.success('已取消收藏')
    } else {
      await addFavorite(product.value.id)
      isFavorited.value = true
      window.$toast?.success('已添加收藏')
    }
  } catch (err) {
    window.$toast?.error(err.message || '操作失败')
  }
}

async function handleBuy() {
  showBuyDialog.value = false
  try {
    await createOrder({ productId: product.value.id, paymentMethod: 'ONLINE' })
    window.$toast?.success('下单成功')
    router.push('/orders')
  } catch (err) {
    window.$toast?.error(err.message || '下单失败')
  }
}

async function handleChat() {
  if (!auth.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    const conv = await getOrCreateConversation({
      otherUserId: product.value.sellerId,
      productId: product.value.id
    })
    router.push(`/chat/${conv.id}`)
  } catch (err) {
    window.$toast?.error(err.message || '创建会话失败')
  }
}

async function handleOffShelf() {
  try {
    await offShelfProduct(product.value.id)
    window.$toast?.success('已下架')
    fetchDetail()
  } catch (err) {
    window.$toast?.error(err.message || '操作失败')
  }
}

async function handleRelist() {
  try {
    await relistProduct(product.value.id)
    window.$toast?.success('已重新上架')
    fetchDetail()
  } catch (err) {
    window.$toast?.error(err.message || '操作失败')
  }
}

async function handleDelete() {
  if (!confirm('确定要删除这个商品吗？')) return
  try {
    await deleteProduct(product.value.id)
    window.$toast?.success('已删除')
    router.push('/my-products')
  } catch (err) {
    window.$toast?.error(err.message || '删除失败')
  }
}

onMounted(fetchDetail)
</script>

<style scoped>
.product-detail-page { padding-bottom: 40px; }

.breadcrumb {
  padding: 16px 0;
  font-size: 13px;
  color: var(--text-muted);
}

.breadcrumb a { color: var(--text-secondary); }
.breadcrumb a:hover { color: var(--primary); }

.detail-main {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
}

.detail-images { }

.main-image {
  width: 100%;
  padding-top: 100%;
  position: relative;
  background: #f0f0f0;
  border-radius: var(--radius);
  overflow: hidden;
}

.main-image img {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  object-fit: cover;
}

.no-image {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.thumb-list {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  overflow-x: auto;
}

.thumb {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  flex-shrink: 0;
}

.thumb.active { border-color: var(--primary); }
.thumb img { width: 100%; height: 100%; object-fit: cover; }

.info-title {
  font-size: 22px;
  font-weight: 600;
  line-height: 1.4;
}

.info-price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 16px;
}

.info-price {
  font-size: 28px;
  color: var(--danger);
  font-weight: 700;
}

.info-meta {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.meta-item {
  display: flex;
  font-size: 14px;
}

.meta-label {
  color: var(--text-muted);
  width: 80px;
  flex-shrink: 0;
}

.meta-value { color: var(--text); }

.seller-card {
  margin-top: 20px;
  padding: 16px;
  background: var(--bg);
  border-radius: var(--radius);
}

.seller-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.seller-avatar {
  width: 44px; height: 44px;
  border-radius: 50%; object-fit: cover;
}

.seller-avatar-placeholder { font-size: 28px; }
.seller-name { font-size: 15px; font-weight: 500; }

.info-actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.detail-section {
  margin-top: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
}

.description-text {
  font-size: 15px;
  line-height: 1.8;
  color: var(--text-secondary);
  white-space: pre-wrap;
}

@media (max-width: 768px) {
  .detail-main { grid-template-columns: 1fr; gap: 20px; }
  .info-title { font-size: 18px; }
  .info-price { font-size: 24px; }
}
</style>
