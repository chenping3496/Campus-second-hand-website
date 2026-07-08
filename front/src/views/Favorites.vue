<template>
  <div class="favorites-page">
    <div class="container">
      <h2 class="page-title">我的收藏</h2>
      <LoadingSpinner v-if="loading" />
      <EmptyState v-else-if="!products.length" icon="💔" title="暂无收藏" description="去首页逛逛，收藏你喜欢的商品吧">
        <router-link to="/" class="btn btn-primary" style="margin-top:16px">去逛逛</router-link>
      </EmptyState>
      <div v-else class="product-grid">
        <ProductCard v-for="product in products" :key="product.id" :product="product" />
      </div>
      <Pagination :current-page="page" :total-pages="totalPages" :total="total" @change="changePage" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFavorites } from '@/api/favorite'
import ProductCard from '@/components/common/ProductCard.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const products = ref([])
const page = ref(0)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const res = await getFavorites({ page: page.value, size: 12 })
    products.value = res.list || []
    total.value = res.total || 0
    totalPages.value = res.totalPages || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function changePage(p) { page.value = p; fetchData() }

onMounted(fetchData)
</script>

<style scoped>
.page-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; }
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 1024px) { .product-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
