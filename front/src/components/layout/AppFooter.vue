<template>
  <footer class="app-footer">
    <div class="container footer-inner">
      <div class="footer-info">
        <span class="footer-logo">🔄 校园二手交易平台</span>
        <p class="footer-desc">专为大学生打造的校园二手物品交易平台</p>
      </div>
      <div class="footer-links">
        <span>安全交易</span>
        <span>实名认证</span>
        <span>校内交易</span>
        <span>品质保障</span>
      </div>
      <div class="footer-copy">
        &copy; {{ year }} 校园二手交易平台 版权所有
      </div>
    </div>
  </footer>
</template>

<script setup>
const year = new Date().getFullYear()
</script>

<style scoped>
.app-footer {
  background: var(--bg-white);
  border-top: 1px solid var(--border-light);
  padding: 24px 0;
  margin-top: 20px;
}

.footer-inner {
  text-align: center;
}

.footer-logo {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary);
}

.footer-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.footer-links {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-secondary);
}

.footer-copy {
  margin-top: 12px;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
