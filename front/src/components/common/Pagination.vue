<template>
  <div class="pagination" v-if="totalPages > 1">
    <button class="page-btn" :disabled="currentPage <= 0" @click="$emit('change', currentPage - 1)">上一页</button>
    <button
      v-for="p in visiblePages"
      :key="p"
      :class="['page-btn', { active: p === currentPage }]"
      @click="$emit('change', p)"
    >{{ p + 1 }}</button>
    <button class="page-btn" :disabled="currentPage >= totalPages - 1" @click="$emit('change', currentPage + 1)">下一页</button>
    <span class="page-info">共 {{ total }} 条</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  currentPage: { type: Number, default: 0 },
  totalPages: { type: Number, default: 0 },
  total: { type: Number, default: 0 }
})

defineEmits(['change'])

const visiblePages = computed(() => {
  const pages = []
  const total = props.totalPages
  const current = props.currentPage
  let start = Math.max(0, current - 2)
  let end = Math.min(total, start + 5)
  if (end - start < 5) {
    start = Math.max(0, end - 5)
  }
  for (let i = start; i < end; i++) {
    pages.push(i)
  }
  return pages
})
</script>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 24px 0;
}

.page-btn {
  padding: 6px 14px;
  border: 1px solid var(--border);
  background: var(--bg-white);
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text);
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled):not(.active) {
  border-color: var(--primary);
  color: var(--primary);
}

.page-btn.active {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: var(--text-muted);
  margin-left: 12px;
}
</style>
