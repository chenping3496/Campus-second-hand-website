<template>
  <nav class="admin-sidebar card">
    <h3 class="sidebar-heading">管理后台</h3>
    <router-link :class="['sidebar-item', { active: current === 'users' }]" to="/admin/users">
      👥 用户管理
    </router-link>
    <router-link :class="['sidebar-item', { active: current === 'products' }]" to="/admin/products">
      📦 商品管理
    </router-link>
    <router-link :class="['sidebar-item', { active: current === 'categories' }]" to="/admin/categories">
      📂 分类管理
    </router-link>
    <router-link :class="['sidebar-item', { active: current === 'orders' }]" to="/admin/orders">
      📋 订单管理
    </router-link>
    <router-link :class="['sidebar-item', { active: current === 'verifications' }]" to="/admin/verifications">
      🛡️ 认证管理
    </router-link>
    <router-link :class="['sidebar-item', { active: current === 'complaints' }]" to="/admin/complaints">
      📝 投诉管理
    </router-link>
  </nav>
</template>

<script setup>
defineProps({
  current: { type: String, default: 'dashboard' }
})
</script>

<style scoped>
.admin-sidebar {
  padding: 16px 0;
  position: sticky;
  top: calc(var(--header-height) + 24px);
}

.sidebar-heading {
  font-size: 16px; font-weight: 600;
  padding: 0 20px 16px;
}

.sidebar-item {
  display: block;
  padding: 10px 20px;
  font-size: 14px;
  color: var(--text-secondary);
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.sidebar-item:hover {
  background: var(--bg);
  color: var(--primary);
}

.sidebar-item.active {
  background: var(--primary-light);
  color: var(--primary-dark);
  border-left-color: var(--primary);
  font-weight: 500;
}

.sidebar-divider {
  height: 1px;
  background: var(--border-light);
  margin: 8px 0;
}

@media (max-width: 768px) {
  .admin-sidebar { display: none; }
}
</style>
