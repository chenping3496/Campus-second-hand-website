<template>
  <div id="app-root">
    <AppHeader />
    <main class="main-content" :class="{ 'admin-main': isAdminRoute }">
      <router-view />
    </main>
    <AppFooter v-if="!isAdminRoute" />
    <ToastNotification />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import ToastNotification from '@/components/common/ToastNotification.vue'

const route = useRoute()
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
</script>

<style scoped>
.main-content {
  min-height: calc(100vh - var(--header-height) - 80px);
  padding-top: var(--header-height);
}

.admin-main {
  min-height: calc(100vh - var(--header-height));
}
</style>
