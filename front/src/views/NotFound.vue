<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <div class="not-found-icon">🔍</div>
      <h1>404</h1>
      <p>页面未找到</p>
      <router-link to="/" class="btn btn-primary">返回首页</router-link>
    </div>
  </div>
</template>

<style scoped>
.not-found-page {
  display: flex; align-items: center; justify-content: center;
  min-height: calc(100vh - var(--header-height) - 80px);
}

.not-found-content { text-align: center; }

.not-found-icon { font-size: 64px; }

h1 { font-size: 72px; font-weight: 700; color: var(--text-muted); }

p { font-size: 18px; color: var(--text-secondary); margin: 12px 0 24px; }
</style>
