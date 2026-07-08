<template>
  <div class="my-products-page">
    <div class="container">
      <div class="page-header">
        <h2>我的发布</h2>
        <router-link to="/publish" class="btn btn-primary">发布商品</router-link>
      </div>

      <div class="status-tabs">
        <button :class="['tab-btn', { active: !statusFilter }]" @click="changeStatus(null)">全部</button>
        <button :class="['tab-btn', { active: statusFilter === 'ON_SALE' }]" @click="changeStatus('ON_SALE')">在售</button>
        <button :class="['tab-btn', { active: statusFilter === 'PENDING' }]" @click="changeStatus('PENDING')">审核中</button>
        <button :class="['tab-btn', { active: statusFilter === 'OFF_SHELF' }]" @click="changeStatus('OFF_SHELF')">已下架</button>
        <button :class="['tab-btn', { active: statusFilter === 'SOLD' }]" @click="changeStatus('SOLD')">已售出</button>
      </div>

      <LoadingSpinner v-if="loading" />
      <EmptyState v-else-if="!products.length" icon="📦" title="暂无商品" description="还没有发布过商品">
        <router-link to="/publish" class="btn btn-primary" style="margin-top:16px">发布第一件商品</router-link>
      </EmptyState>

      <div v-else class="product-list">
        <div v-for="product in products" :key="product.id" class="product-item card">
          <router-link :to="`/product/${product.id}`" class="item-image">
            <img v-if="product.images && product.images.length" :src="product.images[0]" alt="" />
            <div v-else class="no-img">暂无图片</div>
          </router-link>
          <div class="item-info">
            <router-link :to="`/product/${product.id}`" class="item-title">{{ product.title }}</router-link>
            <div class="item-meta">
              <span class="item-price">¥{{ product.price }}</span>
              <span :class="['tag', statusClass(product.status)]">{{ statusText(product.status) }}</span>
            </div>
            <div class="item-stats">
              <span>👁 {{ product.viewCount || 0 }}</span>
              <span>❤️ {{ product.favoriteCount || 0 }}</span>
              <span>{{ formatDate(product.createdAt) }}</span>
            </div>
          </div>
          <div class="item-actions">
            <router-link v-if="product.status === 'ON_SALE'" :to="`/edit-product/${product.id}`" class="btn btn-outline btn-sm">编辑</router-link>
            <button v-if="product.status === 'ON_SALE'" class="btn btn-secondary btn-sm" @click="handleOffShelf(product.id)">下架</button>
            <button v-if="product.status === 'OFF_SHELF'" class="btn btn-primary btn-sm" @click="handleRelist(product.id)">上架</button>
            <button v-if="product.status !== 'ON_SALE'" class="btn btn-danger btn-sm" @click="handleDelete(product.id)">删除</button>
          </div>
        </div>
      </div>

      <Pagination :current-page="page" :total-pages="totalPages" :total="total" @change="changePage" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getMyProducts, offShelfProduct, relistProduct, deleteProduct } from '@/api/product'
import { onMounted } from 'vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const products = ref([])
const statusFilter = ref(null)
const page = ref(0)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)

function statusText(s) {
  const map = { ON_SALE: '在售', SOLD: '已售出', OFF_SHELF: '已下架', PENDING: '审核中', REJECTED: '已拒绝' }
  return map[s] || s
}

function statusClass(s) {
  const map = { ON_SALE: 'tag-success', SOLD: 'tag-default', OFF_SHELF: 'tag-warning', PENDING: 'tag-info', REJECTED: 'tag-danger' }
  return map[s] || 'tag-default'
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN')
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getMyProducts({ status: statusFilter.value || undefined, page: page.value, size: 10 })
    products.value = res.list || []
    total.value = res.total || 0
    totalPages.value = res.totalPages || 0
  } catch (err) {
    window.$toast?.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function changeStatus(s) {
  statusFilter.value = s
  page.value = 0
  fetchData()
}

function changePage(p) {
  page.value = p
  fetchData()
}

async function handleOffShelf(id) {
  try {
    await offShelfProduct(id)
    window.$toast?.success('已下架')
    fetchData()
  } catch (err) { window.$toast?.error(err.message || '操作失败') }
}

async function handleRelist(id) {
  try {
    await relistProduct(id)
    window.$toast?.success('已上架')
    fetchData()
  } catch (err) { window.$toast?.error(err.message || '操作失败') }
}

async function handleDelete(id) {
  if (!confirm('确定删除？')) return
  try {
    await deleteProduct(id)
    window.$toast?.success('已删除')
    fetchData()
  } catch (err) { window.$toast?.error(err.message || '删除失败') }
}

onMounted(fetchData)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 { font-size: 22px; font-weight: 600; }

.status-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  background: var(--bg-white);
  border-radius: var(--radius);
  padding: 4px;
  box-shadow: var(--shadow);
}

.tab-btn {
  flex: 1;
  padding: 8px 0;
  background: none;
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.tab-btn.active {
  background: var(--primary);
  color: #fff;
}

.product-list { display: flex; flex-direction: column; gap: 12px; }

.product-item {
  display: flex;
  gap: 16px;
  padding: 16px;
}

.item-image {
  width: 100px; height: 100px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  flex-shrink: 0;
}

.item-image img { width: 100%; height: 100%; object-fit: cover; }
.no-img { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f0f0f0; color: var(--text-muted); font-size: 12px; }

.item-info { flex: 1; display: flex; flex-direction: column; gap: 6px; min-width: 0; }

.item-title { font-size: 16px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-title:hover { color: var(--primary); }

.item-meta { display: flex; align-items: center; gap: 10px; }

.item-price { font-size: 18px; color: var(--danger); font-weight: 600; }

.item-stats { display: flex; gap: 12px; font-size: 12px; color: var(--text-muted); }

.item-actions { display: flex; gap: 8px; align-items: flex-start; }

@media (max-width: 768px) {
  .product-item { flex-wrap: wrap; }
  .item-actions { width: 100%; justify-content: flex-end; }
}
</style>
