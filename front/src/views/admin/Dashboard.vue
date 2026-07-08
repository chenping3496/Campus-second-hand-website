<template>
  <div class="admin-page">
    <div class="container">
      <div class="admin-layout">
        <AdminSidebar current="dashboard" />
        <div class="admin-content">
          <h2 class="admin-title">数据面板</h2>
          <LoadingSpinner v-if="loading" />
          <div v-else-if="stats" class="dashboard">
            <div class="stat-grid">
              <div class="stat-card card">
                <div class="stat-icon">👥</div>
                <div class="stat-info">
                  <span class="stat-value">{{ stats.userCount }}</span>
                  <span class="stat-label">用户总数</span>
                </div>
              </div>
              <div class="stat-card card">
                <div class="stat-icon">📦</div>
                <div class="stat-info">
                  <span class="stat-value">{{ stats.productCount }}</span>
                  <span class="stat-label">商品总数</span>
                </div>
              </div>
              <div class="stat-card card">
                <div class="stat-icon">🏪</div>
                <div class="stat-info">
                  <span class="stat-value">{{ stats.onSaleProductCount }}</span>
                  <span class="stat-label">在售商品</span>
                </div>
              </div>
              <div class="stat-card card">
                <div class="stat-icon">⏳</div>
                <div class="stat-info">
                  <span class="stat-value">{{ stats.pendingProductCount }}</span>
                  <span class="stat-label">待审核商品</span>
                </div>
              </div>
              <div class="stat-card card">
                <div class="stat-icon">📋</div>
                <div class="stat-info">
                  <span class="stat-value">{{ stats.orderCount }}</span>
                  <span class="stat-label">订单总数</span>
                </div>
              </div>
              <div class="stat-card card">
                <div class="stat-icon">✅</div>
                <div class="stat-info">
                  <span class="stat-value">{{ stats.completedOrderCount }}</span>
                  <span class="stat-label">已完成订单</span>
                </div>
              </div>
              <div class="stat-card card">
                <div class="stat-icon">💰</div>
                <div class="stat-info">
                  <span class="stat-value">¥{{ stats.totalAmount || 0 }}</span>
                  <span class="stat-label">总交易金额</span>
                </div>
              </div>
            </div>

            <!-- Quick links -->
            <div class="quick-links card">
              <h3>快捷操作</h3>
              <div class="links-grid">
                <router-link to="/admin/products" class="quick-link">
                  <span>📦</span> 商品审核
                </router-link>
                <router-link to="/admin/users" class="quick-link">
                  <span>👥</span> 用户管理
                </router-link>
                <router-link to="/admin/categories" class="quick-link">
                  <span>📂</span> 分类管理
                </router-link>
                <router-link to="/admin/orders" class="quick-link">
                  <span>📋</span> 订单管理
                </router-link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getStatistics } from '@/api/admin'
import AdminSidebar from '@/components/admin/AdminSidebar.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const stats = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    stats.value = await getStatistics()
  } catch { /* ignore */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.admin-page { padding: 24px 0 40px; }

.admin-layout {
  display: grid; grid-template-columns: 220px 1fr; gap: 24px;
}

.admin-title { font-size: 22px; font-weight: 600; margin-bottom: 24px; }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex; align-items: center; gap: 14px;
  padding: 20px;
}

.stat-icon { font-size: 32px; }

.stat-value { font-size: 24px; font-weight: 700; display: block; }
.stat-label { font-size: 13px; color: var(--text-muted); }

.quick-links { padding: 24px; }
.quick-links h3 { font-size: 16px; font-weight: 600; margin-bottom: 16px; }

.links-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }

.quick-link {
  display: flex; align-items: center; gap: 8px;
  padding: 16px; background: var(--bg); border-radius: var(--radius);
  font-size: 14px; transition: background 0.2s;
}

.quick-link:hover { background: var(--primary-light); color: var(--primary); }

@media (max-width: 1024px) {
  .stat-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 768px) {
  .admin-layout { grid-template-columns: 1fr; }
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .links-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
