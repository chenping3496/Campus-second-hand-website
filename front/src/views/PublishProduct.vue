<template>
  <div class="publish-page">
    <div class="container">
      <div class="card publish-card">
        <h2 class="publish-title">{{ isEdit ? '编辑商品' : '发布商品' }}</h2>
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>商品标题 *</label>
            <input v-model="form.title" type="text" class="form-input" :class="{ error: errors.title }" placeholder="请输入商品标题" />
            <p v-if="errors.title" class="form-error">{{ errors.title }}</p>
          </div>
          <div class="form-group">
            <label>商品分类 *</label>
            <select v-model="form.categoryId" class="form-input" :class="{ error: errors.categoryId }">
              <option :value="null" disabled>请选择分类</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
            <p v-if="errors.categoryId" class="form-error">{{ errors.categoryId }}</p>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>售价 *</label>
              <input v-model.number="form.price" type="number" step="0.01" class="form-input" :class="{ error: errors.price }" placeholder="¥" />
              <p v-if="errors.price" class="form-error">{{ errors.price }}</p>
            </div>
            <div class="form-group">
              <label>原价</label>
              <input v-model.number="form.originalPrice" type="number" step="0.01" class="form-input" placeholder="选填" />
            </div>
          </div>
          <div class="form-group">
            <label>商品描述</label>
            <textarea v-model="form.description" class="form-textarea" placeholder="描述一下商品的状态、使用情况等信息..." rows="5"></textarea>
          </div>
          <div class="form-group">
            <label>商品图片</label>
            <ImageUpload v-model="form.images" />
          </div>
          <div class="form-actions">
            <router-link to="/my-products" class="btn btn-secondary">取消</router-link>
            <button type="submit" class="btn btn-primary btn-lg" :disabled="submitting">
              {{ submitting ? '提交中...' : isEdit ? '保存修改' : '发布商品' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createProduct, updateProduct, getProductDetail } from '@/api/product'
import { getCategories } from '@/api/category'
import ImageUpload from '@/components/common/ImageUpload.vue'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const categories = ref([])
const submitting = ref(false)

const form = reactive({
  title: '',
  categoryId: null,
  price: null,
  originalPrice: null,
  description: '',
  images: []
})

const errors = reactive({})

function validate() {
  errors.title = ''
  errors.categoryId = ''
  errors.price = ''
  if (!form.title.trim()) { errors.title = '请输入商品标题'; return false }
  if (!form.categoryId) { errors.categoryId = '请选择商品分类'; return false }
  if (!form.price || form.price <= 0) { errors.price = '请输入有效的价格'; return false }
  return true
}

async function handleSubmit() {
  if (!validate()) return
  submitting.value = true
  try {
    const data = {
      title: form.title,
      categoryId: form.categoryId,
      price: form.price,
      originalPrice: form.originalPrice || undefined,
      description: form.description || undefined,
      images: form.images.length ? form.images : undefined
    }
    if (isEdit.value) {
      await updateProduct(route.params.id, data)
      window.$toast?.success('修改成功')
    } else {
      await createProduct(data)
      window.$toast?.success('发布成功')
    }
    router.push('/my-products')
  } catch (err) {
    window.$toast?.error(err.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    categories.value = await getCategories()
  } catch { /* ignore */ }

  if (isEdit.value) {
    try {
      const product = await getProductDetail(route.params.id)
      form.title = product.title
      form.categoryId = product.categoryId
      form.price = product.price
      form.originalPrice = product.originalPrice
      form.description = product.description || ''
      form.images = product.images || []
    } catch {
      window.$toast?.error('加载商品信息失败')
      router.push('/my-products')
    }
  }
})
</script>

<style scoped>
.publish-page { padding: 24px 0 40px; }

.publish-card {
  max-width: 660px;
  margin: 0 auto;
  padding: 32px;
}

.publish-title {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 24px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .publish-card { padding: 20px; }
  .form-row { grid-template-columns: 1fr; }
}
</style>
