<template>
  <Teleport to="body">
    <div v-for="toast in toasts" :key="toast.id" :class="['toast', `toast-${toast.type}`]">
      {{ toast.message }}
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'

const toasts = ref([])
let idCounter = 0

function addToast(message, type = 'info', duration = 3000) {
  const id = ++idCounter
  toasts.value.push({ id, message, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }, duration)
}

// Expose for global use
window.$toast = {
  success: (msg) => addToast(msg, 'success'),
  error: (msg) => addToast(msg, 'error'),
  warning: (msg) => addToast(msg, 'warning'),
  info: (msg) => addToast(msg, 'info')
}
</script>
