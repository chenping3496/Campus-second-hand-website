<template>
  <div class="image-upload">
    <div class="image-list">
      <div
        v-for="(img, index) in modelValue"
        :key="index"
        class="image-item"
      >
        <img :src="img" alt="" />
        <button class="image-remove" @click="removeImage(index)">×</button>
      </div>
      <div v-if="modelValue.length < max" class="upload-trigger" @click="triggerUpload">
        <span class="upload-icon">+</span>
        <span class="upload-text">上传图片</span>
        <input
          ref="fileInput"
          type="file"
          accept="image/*"
          multiple
          class="file-input-hidden"
          @change="handleFileChange"
        />
      </div>
    </div>
    <p class="upload-hint">最多上传{{ max }}张图片，支持jpg/png/gif格式</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { uploadImage } from '@/api/file'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  max: { type: Number, default: 9 }
})

const emit = defineEmits(['update:modelValue'])
const fileInput = ref(null)
const uploading = ref(false)

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(e) {
  const files = Array.from(e.target.files)
  if (!files.length) return

  const remaining = props.max - props.modelValue.length
  const filesToUpload = files.slice(0, remaining)

  uploading.value = true
  try {
    const urls = await Promise.all(filesToUpload.map(f => uploadImage(f)))
    const newImages = [...props.modelValue, ...urls]
    emit('update:modelValue', newImages)
    window.$toast?.success('图片上传成功')
  } catch {
    window.$toast?.error('图片上传失败')
  } finally {
    uploading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

function removeImage(index) {
  const newImages = props.modelValue.filter((_, i) => i !== index)
  emit('update:modelValue', newImages)
}
</script>

<style scoped>
.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid var(--border-light);
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 22px;
  height: 22px;
  background: rgba(0,0,0,0.5);
  color: #fff;
  border-radius: 50%;
  font-size: 14px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-remove:hover {
  background: var(--danger);
}

.upload-trigger {
  width: 100px;
  height: 100px;
  border: 2px dashed var(--border);
  border-radius: var(--radius);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s;
  position: relative;
}

.upload-trigger:hover {
  border-color: var(--primary);
}

.upload-icon {
  font-size: 28px;
  color: var(--text-muted);
}

.upload-text {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.file-input-hidden {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.upload-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
}
</style>
