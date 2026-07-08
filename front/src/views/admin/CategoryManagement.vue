<template>
  <div class="admin-page">
    <div class="container">
      <div class="admin-layout">
        <AdminSidebar current="categories" />
        <div class="admin-content">
          <div class="page-header-row">
            <h2 class="admin-title">分类管理</h2>
            <button class="btn btn-primary" @click="showAddDialog">添加分类</button>
          </div>

          <LoadingSpinner v-if="loading" />
          <EmptyState v-else-if="!categories.length" icon="📂" title="暂无分类" />

          <div v-else class="table-wrapper card">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th><th>图标</th><th>名称</th><th>排序</th><th>状态</th><th>创建时间</th><th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in categories" :key="c.id">
                  <td>{{ c.id }}</td>
                  <td><span class="cat-icon">{{ c.icon || '📂' }}</span></td>
                  <td>{{ c.name }}</td>
                  <td>{{ c.sortOrder }}</td>
                  <td><span :class="['tag', c.enabled ? 'tag-success' : 'tag-default']">{{ c.enabled ? '启用' : '禁用' }}</span></td>
                  <td>{{ formatDate(c.createdAt) }}</td>
                  <td class="actions-cell">
                    <button class="btn btn-outline btn-sm" @click="startEdit(c)">编辑</button>
                    <button class="btn btn-danger btn-sm" @click="handleDelete(c)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Add/Edit dialog -->
          <div v-if="dialogVisible" class="dialog-overlay" @click.self="closeDialog">
            <div class="dialog-card">
              <h3>{{ isEdit ? '编辑分类' : '添加分类' }}</h3>
              <form @submit.prevent="handleSave">
                <div class="form-group">
                  <label>名称 *</label>
                  <input v-model="form.name" type="text" class="form-input" placeholder="分类名称" />
                </div>
                <div class="form-group">
                  <label>图标 (emoji)</label>
                  <input v-model="form.icon" type="text" class="form-input" placeholder="如: 📱" />
                </div>
                <div class="form-group">
                  <label>排序</label>
                  <input v-model.number="form.sortOrder" type="number" class="form-input" placeholder="数字越小越靠前" />
                </div>
                <div class="form-group">
                  <label>
                    <input v-model="form.enabled" type="checkbox" /> 启用
                  </label>
                </div>
                <div class="dialog-actions">
                  <button type="button" class="btn btn-secondary" @click="closeDialog">取消</button>
                  <button type="submit" class="btn btn-primary" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAdminCategories, createCategory, updateCategory, deleteCategory } from '@/api/admin'
import AdminSidebar from '@/components/admin/AdminSidebar.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const categories = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)

const form = reactive({ name: '', icon: '', sortOrder: 0, enabled: true })

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('zh-CN')
}

async function fetchData() {
  loading.value = true
  try { categories.value = await getAdminCategories() }
  catch (err) { window.$toast?.error(err.message || '加载失败') }
  finally { loading.value = false }
}

function showAddDialog() {
  isEdit.value = false; editId.value = null
  form.name = ''; form.icon = ''; form.sortOrder = 0; form.enabled = true
  dialogVisible.value = true
}

function startEdit(c) {
  isEdit.value = true; editId.value = c.id
  form.name = c.name; form.icon = c.icon || ''; form.sortOrder = c.sortOrder || 0; form.enabled = c.enabled
  dialogVisible.value = true
}

function closeDialog() { dialogVisible.value = false }

async function handleSave() {
  if (!form.name.trim()) { window.$toast?.error('请输入分类名称'); return }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateCategory(editId.value, { name: form.name, icon: form.icon || undefined, sortOrder: form.sortOrder, enabled: form.enabled })
      window.$toast?.success('已更新')
    } else {
      await createCategory({ name: form.name, icon: form.icon || undefined, sortOrder: form.sortOrder, enabled: form.enabled })
      window.$toast?.success('已创建')
    }
    closeDialog()
    fetchData()
  } catch (err) { window.$toast?.error(err.message || '保存失败') }
  finally { saving.value = false }
}

async function handleDelete(c) {
  if (!confirm(`确定删除分类「${c.name}」吗？`)) return
  try { await deleteCategory(c.id); window.$toast?.success('已删除'); fetchData() }
  catch (err) { window.$toast?.error(err.message || '删除失败') }
}

onMounted(fetchData)
</script>

<style scoped>
.admin-page { padding: 24px 0 40px; }
.admin-layout { display: grid; grid-template-columns: 220px 1fr; gap: 24px; }

.page-header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.admin-title { font-size: 22px; font-weight: 600; margin-bottom: 0; }

.table-wrapper { padding: 0; overflow-x: auto; }

.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th, .data-table td { padding: 12px 14px; text-align: left; border-bottom: 1px solid var(--border-light); white-space: nowrap; }
.data-table th { background: var(--bg); font-weight: 600; color: var(--text-secondary); }
.data-table tr:hover td { background: #fafafa; }

.cat-icon { font-size: 20px; }
.actions-cell { display: flex; gap: 6px; }

.dialog-overlay {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center;
  z-index: 9999;
}

.dialog-card {
  background: var(--bg-white); border-radius: var(--radius-lg);
  padding: 28px; max-width: 440px; width: 90%;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}

.dialog-card h3 { font-size: 18px; font-weight: 600; margin-bottom: 20px; }

.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 24px; }

@media (max-width: 768px) {
  .admin-layout { grid-template-columns: 1fr; }
}
</style>
