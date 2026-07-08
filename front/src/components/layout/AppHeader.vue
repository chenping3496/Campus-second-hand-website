<template>
  <!-- Admin header -->
  <header v-if="auth.isAdmin" class="app-header admin-header">
    <div class="header-inner container">
      <router-link to="/admin" class="logo">
        <span class="logo-icon">⚙️</span>
        <span class="logo-text">后台管理</span>
      </router-link>

      <nav class="nav-links admin-nav">
        <router-link to="/admin" class="nav-link">数据面板</router-link>
        <router-link to="/admin/users" class="nav-link">用户管理</router-link>
        <router-link to="/admin/products" class="nav-link">商品管理</router-link>
        <router-link to="/admin/categories" class="nav-link">分类管理</router-link>
        <router-link to="/admin/orders" class="nav-link">订单管理</router-link>
        <div class="admin-user-area">
          <span class="admin-badge">管理员</span>
          <span class="admin-name">{{ auth.username }}</span>
          <a class="nav-link logout-link" @click="handleLogout">退出登录</a>
        </div>
      </nav>

      <button class="mobile-menu-btn" @click="showMobileMenu = !showMobileMenu">
        <span></span><span></span><span></span>
      </button>
    </div>

    <!-- Admin mobile menu -->
    <div v-if="showMobileMenu" class="mobile-menu" @click.self="showMobileMenu = false">
      <div class="mobile-menu-content">
        <router-link to="/admin" class="mobile-link" @click="showMobileMenu = false">数据面板</router-link>
        <router-link to="/admin/users" class="mobile-link" @click="showMobileMenu = false">用户管理</router-link>
        <router-link to="/admin/products" class="mobile-link" @click="showMobileMenu = false">商品管理</router-link>
        <router-link to="/admin/categories" class="mobile-link" @click="showMobileMenu = false">分类管理</router-link>
        <router-link to="/admin/orders" class="mobile-link" @click="showMobileMenu = false">订单管理</router-link>
        <a class="mobile-link logout" @click="handleLogout">退出登录</a>
      </div>
    </div>
  </header>

  <!-- Regular user header -->
  <header v-else class="app-header">
    <div class="header-inner container">
      <router-link to="/" class="logo">
        <span class="logo-icon">🔄</span>
        <span class="logo-text">校园二手</span>
      </router-link>

      <div class="search-bar" v-if="showSearch">
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索商品..."
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <button class="search-btn" @click="handleSearch">搜索</button>
      </div>

      <nav class="nav-links">
        <router-link to="/" class="nav-link">首页</router-link>
        <template v-if="auth.isLoggedIn">
          <router-link v-if="needsVerification" to="/verification" class="nav-link verify-warn">
            ⚠️ 请认证
          </router-link>
          <router-link to="/chat" class="nav-link">
            消息
            <span v-if="unreadChatCount" class="badge">{{ unreadChatCount > 99 ? '99+' : unreadChatCount }}</span>
          </router-link>
          <router-link to="/notifications" class="nav-link">
            通知
            <span v-if="unreadNotifCount" class="badge">{{ unreadNotifCount > 99 ? '99+' : unreadNotifCount }}</span>
          </router-link>
          <div class="user-menu" ref="userMenuRef">
            <div class="user-trigger" @click="toggleUserMenu">
              <img v-if="auth.avatar" :src="auth.avatar" class="user-avatar" alt="" />
              <span v-else class="user-avatar-placeholder">{{ auth.username.charAt(0) }}</span>
              <span class="user-name">{{ auth.username }}</span>
              <span class="arrow">▾</span>
            </div>
            <div v-if="showUserMenu" class="user-dropdown">
              <router-link to="/profile" class="dropdown-item" @click="showUserMenu = false">个人中心</router-link>
              <router-link to="/my-products" class="dropdown-item" @click="showUserMenu = false">我的发布</router-link>
              <router-link to="/favorites" class="dropdown-item" @click="showUserMenu = false">我的收藏</router-link>
              <router-link to="/orders" class="dropdown-item" @click="showUserMenu = false">我的订单</router-link>
              <router-link to="/complaints" class="dropdown-item" @click="showUserMenu = false">我的投诉</router-link>
              <div class="dropdown-divider"></div>
              <a class="dropdown-item logout" @click="handleLogout">退出登录</a>
            </div>
          </div>
          <router-link to="/publish" class="btn btn-primary btn-sm publish-btn">发布商品</router-link>
        </template>
        <template v-else>
          <router-link to="/login" class="nav-link">登录</router-link>
          <router-link to="/register" class="btn btn-primary btn-sm">注册</router-link>
        </template>
      </nav>

      <button class="mobile-menu-btn" @click="showMobileMenu = !showMobileMenu">
        <span></span><span></span><span></span>
      </button>
    </div>

    <!-- Mobile menu -->
    <div v-if="showMobileMenu" class="mobile-menu" @click.self="showMobileMenu = false">
      <div class="mobile-menu-content">
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索商品..."
          class="mobile-search-input"
          @keyup.enter="handleSearch"
        />
        <router-link to="/" class="mobile-link" @click="showMobileMenu = false">首页</router-link>
        <template v-if="auth.isLoggedIn">
          <router-link to="/chat" class="mobile-link" @click="showMobileMenu = false">消息</router-link>
          <router-link to="/notifications" class="mobile-link" @click="showMobileMenu = false">通知</router-link>
          <router-link to="/publish" class="mobile-link" @click="showMobileMenu = false">发布商品</router-link>
          <router-link to="/profile" class="mobile-link" @click="showMobileMenu = false">个人中心</router-link>
          <router-link to="/my-products" class="mobile-link" @click="showMobileMenu = false">我的发布</router-link>
          <router-link to="/favorites" class="mobile-link" @click="showMobileMenu = false">我的收藏</router-link>
          <router-link to="/orders" class="mobile-link" @click="showMobileMenu = false">我的订单</router-link>
          <router-link to="/complaints" class="mobile-link" @click="showMobileMenu = false">我的投诉</router-link>
          <a class="mobile-link logout" @click="handleLogout">退出登录</a>
        </template>
        <template v-else>
          <router-link to="/login" class="mobile-link" @click="showMobileMenu = false">登录</router-link>
          <router-link to="/register" class="mobile-link" @click="showMobileMenu = false">注册</router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getUnreadCount as getChatUnread } from '@/api/chat'
import { getUnreadNotificationCount } from '@/api/notification'
import { getVerificationStatus } from '@/api/verification'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const searchKeyword = ref('')
const showUserMenu = ref(false)
const showMobileMenu = ref(false)
const userMenuRef = ref(null)
const unreadChatCount = ref(0)
const unreadNotifCount = ref(0)
const verificationStatus = ref('')

const needsVerification = computed(() => {
  return auth.isLoggedIn && !auth.isAdmin && verificationStatus.value &&
    verificationStatus.value !== 'APPROVED'
})

const showSearch = computed(() => {
  return route.name !== 'Home'
})

function handleSearch() {
  if (searchKeyword.value.trim()) {
    showMobileMenu.value = false
    router.push({ name: 'Home', query: { keyword: searchKeyword.value.trim() } })
  }
}

function toggleUserMenu() {
  showUserMenu.value = !showUserMenu.value
}

function handleClickOutside(e) {
  if (userMenuRef.value && !userMenuRef.value.contains(e.target)) {
    showUserMenu.value = false
  }
}

function handleLogout() {
  auth.logout()
  showUserMenu.value = false
  showMobileMenu.value = false
  router.push('/login')
}

async function fetchUnreadCounts() {
  if (auth.isLoggedIn && !auth.isAdmin) {
    try {
      const [chatCount, notifCount, verifyStatus] = await Promise.all([
        getChatUnread(),
        getUnreadNotificationCount(),
        getVerificationStatus()
      ])
      unreadChatCount.value = chatCount
      unreadNotifCount.value = notifCount
      verificationStatus.value = verifyStatus.verificationStatus
    } catch { /* ignore */ }
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  fetchUnreadCounts()
  const interval = setInterval(fetchUnreadCounts, 30000)
  onUnmounted(() => {
    document.removeEventListener('click', handleClickOutside)
    clearInterval(interval)
  })
})
</script>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: var(--header-height);
  background: var(--bg-white);
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  z-index: 1000;
}

.admin-header {
  background: #1a1a2e;
}

.admin-header .logo {
  color: #fff;
}

.header-inner {
  display: flex;
  align-items: center;
  height: 100%;
  gap: 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 20px;
  font-weight: 700;
  color: var(--primary);
  flex-shrink: 0;
}
.logo-icon { font-size: 24px; }

.search-bar {
  flex: 1;
  max-width: 400px;
  display: flex;
}

.search-input {
  flex: 1;
  padding: 8px 14px;
  border: 1px solid var(--border);
  border-right: none;
  border-radius: var(--radius) 0 0 var(--radius);
  font-size: 14px;
}

.search-input:focus {
  border-color: var(--primary);
}

.search-btn {
  padding: 8px 16px;
  background: var(--primary);
  color: #fff;
  border-radius: 0 var(--radius) var(--radius) 0;
  font-size: 14px;
}

/* Admin nav */
.admin-nav {
  margin-left: auto;
}

.admin-nav .nav-link {
  color: rgba(255,255,255,0.75) !important;
}

.admin-nav .nav-link:hover,
.admin-nav .nav-link.router-link-active {
  color: #fff !important;
  background: rgba(255,255,255,0.1);
}

.admin-user-area {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: 16px;
  padding-left: 16px;
  border-left: 1px solid rgba(255,255,255,0.2);
}

.admin-badge {
  background: #e65100;
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

.admin-name {
  color: rgba(255,255,255,0.9);
  font-size: 14px;
}

.logout-link {
  color: rgba(255,255,255,0.6) !important;
  cursor: pointer;
}

.logout-link:hover {
  color: #ff5252 !important;
}

/* Regular nav */
.nav-links {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.nav-link {
  padding: 8px 12px;
  font-size: 14px;
  color: var(--text-secondary);
  border-radius: var(--radius);
  transition: all 0.2s;
  position: relative;
}

.nav-link:hover, .nav-link.router-link-active {
  color: var(--primary);
  background: var(--primary-light);
}

.badge {
  position: absolute;
  top: 2px;
  right: 0;
  min-width: 18px;
  height: 18px;
  background: var(--danger);
  color: #fff;
  font-size: 11px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.user-menu {
  position: relative;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  cursor: pointer;
  border-radius: var(--radius);
  transition: background 0.2s;
}

.user-trigger:hover {
  background: var(--bg);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.user-avatar-placeholder {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow {
  font-size: 10px;
  color: var(--text-muted);
}

.user-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: var(--bg-white);
  border-radius: var(--radius);
  box-shadow: var(--shadow-hover);
  min-width: 150px;
  padding: 8px 0;
  z-index: 100;
}

.dropdown-item {
  display: block;
  padding: 8px 16px;
  font-size: 14px;
  color: var(--text);
  transition: background 0.2s;
}

.dropdown-item:hover {
  background: var(--bg);
}

.logout {
  color: var(--danger) !important;
}

.dropdown-divider {
  height: 1px;
  background: var(--border-light);
  margin: 4px 0;
}

.verify-warn {
  color: #E65100 !important;
  font-weight: 600;
  animation: blink 1.5s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.publish-btn {
  margin-left: 4px;
}

.mobile-menu-btn {
  display: none;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  background: none;
}

.mobile-menu-btn span {
  width: 20px;
  height: 2px;
  background: var(--text);
  border-radius: 2px;
}

.admin-header .mobile-menu-btn span {
  background: #fff;
}

.mobile-menu {
  display: none;
  position: fixed;
  top: var(--header-height);
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.4);
  z-index: 999;
}

.mobile-menu-content {
  background: var(--bg-white);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 80vh;
  overflow-y: auto;
}

.mobile-search-input {
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 14px;
  margin-bottom: 8px;
}

.mobile-link {
  padding: 12px 8px;
  font-size: 15px;
  color: var(--text);
  border-radius: var(--radius);
}

.mobile-link:hover {
  background: var(--bg);
}

.mobile-link.logout {
  color: var(--danger);
}

@media (max-width: 768px) {
  .search-bar { display: none; }
  .nav-links { display: none; }
  .admin-nav { display: none; }
  .mobile-menu-btn { display: flex; }
  .mobile-menu { display: block; }
}
</style>
