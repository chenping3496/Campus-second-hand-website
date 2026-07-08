<template>
  <div class="home-page">
    <!-- Hero search section -->
    <div class="hero-section">
      <div class="container">
        <h1 class="hero-title">校园二手交易平台</h1>
        <p class="hero-subtitle">在校园内安全、便捷地买卖二手物品</p>
        <div class="hero-search">
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索你想要的商品..."
            class="hero-search-input"
            @keyup.enter="doSearch"
          />
          <button class="btn btn-primary btn-lg" @click="doSearch">🔍 搜索</button>
        </div>
      </div>
    </div>

    <div class="container">
      <!-- Categories -->
      <div class="categories-bar" v-if="categories.length">
        <button
          :class="['cat-btn', { active: !selectedCategory }]"
          @click="selectCategory(null)"
        >全部</button>
        <button
          v-for="cat in categories"
          :key="cat.id"
          :class="['cat-btn', { active: selectedCategory === cat.id }]"
          @click="selectCategory(cat.id)"
        >
          <span v-if="cat.icon" class="cat-icon">{{ cat.icon }}</span>
          {{ cat.name }}
        </button>
      </div>

      <!-- Sort bar -->
      <div class="sort-bar">
        <span class="result-count">共 {{ total }} 件商品</span>
        <div class="sort-options">
          <button :class="['sort-btn', { active: sortBy === 'createdAt' && sortDir === 'desc' }]" @click="setSort('createdAt', 'desc')">最新</button>
          <button :class="['sort-btn', { active: sortBy === 'price' && sortDir === 'asc' }]" @click="setSort('price', 'asc')">价格↑</button>
          <button :class="['sort-btn', { active: sortBy === 'price' && sortDir === 'desc' }]" @click="setSort('price', 'desc')">价格↓</button>
          <button :class="['sort-btn', { active: sortBy === 'viewCount' && sortDir === 'desc' }]" @click="setSort('viewCount', 'desc')">最热</button>
        </div>
      </div>

      <!-- Product grid -->
      <LoadingSpinner v-if="loading" />
      <EmptyState v-else-if="!products.length" icon="🔍" title="暂无商品" description="暂时没有找到相关商品" />

      <div v-else class="product-grid">
        <ProductCard v-for="product in products" :key="product.id" :product="product" />
      </div>

      <Pagination
        :current-page="page"
        :total-pages="totalPages"
        :total="total"
        @change="changePage"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchProducts } from '@/api/product'
import { getCategories } from '@/api/category'
import ProductCard from '@/components/common/ProductCard.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const selectedCategory = ref(null)
const sortBy = ref('createdAt')
const sortDir = ref('desc')
const page = ref(0)
const size = 12

const products = ref([])
const categories = ref([])
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)

async function fetchCategories() {
  try {
    categories.value = await getCategories()
  } catch { /* ignore */ }
}

async function fetchProducts() {
  loading.value = true
  try {
    const params = {
      keyword: keyword.value || undefined,
      categoryId: selectedCategory.value || undefined,
      page: page.value,
      size,
      sortBy: sortBy.value,
      sortDir: sortDir.value
    }
    const res = await searchProducts(params)
    products.value = res.list || []
    total.value = res.total || 0
    totalPages.value = res.totalPages || 0
  } catch (err) {
    window.$toast?.error(err.message || '加载商品失败')
  } finally {
    loading.value = false
  }
}

function doSearch() {
  page.value = 0
  router.replace({ query: { keyword: keyword.value || undefined } })
  fetchProducts()
}

function selectCategory(id) {
  selectedCategory.value = id
  page.value = 0
  fetchProducts()
}

function setSort(by, dir) {
  sortBy.value = by
  sortDir.value = dir
  page.value = 0
  fetchProducts()
}

function changePage(p) {
  page.value = p
  fetchProducts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  if (route.query.keyword) {
    keyword.value = route.query.keyword
  }
  fetchCategories()
  fetchProducts()
})

watch(() => route.query.keyword, (val) => {
  if (val) keyword.value = val
})
</script>

<style scoped>
.hero-section {
  background: linear-gradient(135deg, #43A047, #2E7D32);
  color: #fff;
  padding: 48px 0;
  text-align: center;
  margin-bottom: 24px;
}

.hero-title {
  font-size: 32px;
  font-weight: 700;
}

.hero-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin-top: 8px;
}

.hero-search {
  display: flex;
  max-width: 500px;
  margin: 24px auto 0;
  gap: 0;
}

.hero-search-input {
  flex: 1;
  padding: 14px 20px;
  border: none;
  border-radius: var(--radius) 0 0 var(--radius);
  font-size: 16px;
}

.hero-search .btn {
  border-radius: 0 var(--radius) var(--radius) 0;
}

.categories-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.cat-btn {
  padding: 8px 16px;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 20px;
  font-size: 13px;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.cat-btn:hover { border-color: var(--primary); color: var(--primary); }
.cat-btn.active { background: var(--primary); color: #fff; border-color: var(--primary); }

.cat-icon { margin-right: 4px; }

.sort-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 0 4px;
}

.result-count {
  font-size: 14px;
  color: var(--text-muted);
}

.sort-options {
  display: flex;
  gap: 4px;
}

.sort-btn {
  padding: 6px 14px;
  background: none;
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.sort-btn:hover { color: var(--primary); }
.sort-btn.active { color: var(--primary); font-weight: 600; }

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 1024px) {
  .product-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 768px) {
  .hero-title { font-size: 24px; }
  .hero-subtitle { font-size: 14px; }
  .hero-section { padding: 32px 0; }
  .product-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
}

@media (max-width: 480px) {
  .product-grid { grid-template-columns: 1fr 1fr; gap: 8px; }
  .hero-search { flex-direction: column; }
  .hero-search-input { border-radius: var(--radius); }
  .hero-search .btn { border-radius: var(--radius); margin-top: 8px; }
}
</style>
