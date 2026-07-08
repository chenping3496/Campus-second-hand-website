<template>
  <div class="verification-page">
    <div class="container">
      <div class="card verification-card">
        <h2 class="page-title">身份认证</h2>
        <p class="page-desc">为了保障交易安全，发布商品和联系卖家前需要完成学生/教师身份认证</p>

        <!-- Status display -->
        <div v-if="statusInfo" class="status-display" :class="'status-' + statusInfo.verificationStatus.toLowerCase()">
          <div class="status-icon">{{ statusIcon }}</div>
          <div class="status-text">
            <h3>{{ statusTitle }}</h3>
            <p>{{ statusDesc }}</p>
            <p v-if="statusInfo.verificationRemark" class="remark">备注：{{ statusInfo.verificationRemark }}</p>
          </div>
        </div>

        <!-- Submit form (only show when UNVERIFIED or REJECTED) -->
        <div v-if="canSubmit" class="verification-form-section">
          <div class="divider"></div>
          <h3>{{ statusInfo?.verificationStatus === 'REJECTED' ? '重新提交认证' : '提交身份认证' }}</h3>
          <form @submit.prevent="handleSubmit">
            <div class="form-group">
              <label>真实姓名 *</label>
              <input v-model="form.realName" type="text" class="form-input" placeholder="请输入真实姓名" />
            </div>
            <div class="form-group">
              <label>身份类型 *</label>
              <div class="identity-type-options">
                <label :class="['identity-option', { active: form.identityType === 'STUDENT' }]">
                  <input v-model="form.identityType" type="radio" value="STUDENT" />
                  <span class="identity-label">🎓 学生</span>
                </label>
                <label :class="['identity-option', { active: form.identityType === 'TEACHER' }]">
                  <input v-model="form.identityType" type="radio" value="TEACHER" />
                  <span class="identity-label">👨‍🏫 教师</span>
                </label>
              </div>
            </div>
            <div class="form-group">
              <label>{{ form.identityType === 'TEACHER' ? '工号' : '学号' }} *</label>
              <input v-model="form.identityNumber" type="text" class="form-input" :placeholder="form.identityType === 'TEACHER' ? '请输入工号' : '请输入学号'" />
            </div>
            <div class="form-group">
              <label>证件照上传</label>
              <ImageUpload v-model="form.idCardImage" :max="1" />
              <p class="input-hint">请上传学生证/校园卡/教师证等有效证件照片</p>
            </div>
            <button type="submit" class="btn btn-primary btn-lg" :disabled="submitting" style="width:100%">
              {{ submitting ? '提交中...' : '提交认证' }}
            </button>
          </form>
        </div>

        <!-- Appeal section (only when REJECTED) -->
        <div v-if="statusInfo?.verificationStatus === 'REJECTED' && !statusInfo?.hasPendingAppeal" class="appeal-section">
          <div class="divider"></div>
          <h3>提交申诉</h3>
          <p class="appeal-desc">如果您认为认证驳回有误，可以提交申诉说明情况</p>
          <form @submit.prevent="handleAppeal">
            <div class="form-group">
              <label>申诉原因 *</label>
              <textarea v-model="appealForm.reason" class="form-textarea" placeholder="请详细说明申诉原因..." rows="4"></textarea>
            </div>
            <div class="form-group">
              <label>补充材料（可选）</label>
              <ImageUpload v-model="appealForm.image" :max="1" />
            </div>
            <button type="submit" class="btn btn-warning btn-lg" :disabled="appealing" style="width:100%">
              {{ appealing ? '提交中...' : '提交申诉' }}
            </button>
          </form>
        </div>

        <!-- Frozen notice -->
        <div v-if="statusInfo?.verificationStatus === 'FROZEN'" class="frozen-notice">
          <p>⚠️ 您的账号认证已被冻结，如有疑问请联系管理员。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { getVerificationStatus, submitVerification, submitAppeal } from '@/api/verification'
import ImageUpload from '@/components/common/ImageUpload.vue'

const statusInfo = ref(null)
const submitting = ref(false)
const appealing = ref(false)

const form = reactive({
  realName: '',
  identityType: 'STUDENT',
  identityNumber: '',
  idCardImage: []
})

const appealForm = reactive({
  reason: '',
  image: []
})

const canSubmit = computed(() => {
  if (!statusInfo.value) return false
  const s = statusInfo.value.verificationStatus
  return s === 'UNVERIFIED' || s === 'REJECTED'
})

const statusIcon = computed(() => {
  const map = {
    UNVERIFIED: '❓', PENDING: '⏳', APPROVED: '✅', REJECTED: '❌', FROZEN: '🔒'
  }
  return map[statusInfo.value?.verificationStatus] || '❓'
})

const statusTitle = computed(() => {
  const map = {
    UNVERIFIED: '未认证', PENDING: '认证审核中', APPROVED: '已认证通过',
    REJECTED: '认证未通过', FROZEN: '认证已冻结'
  }
  return map[statusInfo.value?.verificationStatus] || '未知'
})

const statusDesc = computed(() => {
  const map = {
    UNVERIFIED: '您还未完成身份认证，请提交认证信息',
    PENDING: '您的认证申请正在审核中，请耐心等待',
    APPROVED: '恭喜！您已完成身份认证，可以使用全部功能',
    REJECTED: '您的认证申请未通过审核',
    FROZEN: '您的认证已被管理员冻结'
  }
  return map[statusInfo.value?.verificationStatus] || ''
})

async function fetchStatus() {
  try {
    statusInfo.value = await getVerificationStatus()
  } catch (err) {
    window.$toast?.error('获取认证状态失败')
  }
}

async function handleSubmit() {
  if (!form.realName.trim()) return window.$toast?.warning('请输入真实姓名')
  if (!form.identityNumber.trim()) return window.$toast?.warning('请输入学号/工号')

  submitting.value = true
  try {
    await submitVerification({
      realName: form.realName,
      identityType: form.identityType,
      identityNumber: form.identityNumber,
      idCardImage: form.idCardImage.length ? form.idCardImage[0] : null
    })
    window.$toast?.success('认证信息已提交，请等待审核')
    fetchStatus()
  } catch (err) {
    window.$toast?.error(err.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function handleAppeal() {
  if (!appealForm.reason.trim()) return window.$toast?.warning('请输入申诉原因')
  appealing.value = true
  try {
    await submitAppeal({
      reason: appealForm.reason,
      image: appealForm.image.length ? appealForm.image[0] : null
    })
    window.$toast?.success('申诉已提交')
    fetchStatus()
  } catch (err) {
    window.$toast?.error(err.message || '申诉提交失败')
  } finally {
    appealing.value = false
  }
}

onMounted(fetchStatus)
</script>

<style scoped>
.verification-page { padding: 40px 0; }
.verification-card { max-width: 560px; margin: 0 auto; padding: 32px; }

.page-title { font-size: 24px; font-weight: 700; }
.page-desc { font-size: 14px; color: var(--text-secondary); margin-top: 8px; }

.status-display {
  display: flex; gap: 16px; align-items: center;
  padding: 20px; border-radius: var(--radius); margin-top: 24px;
}

.status-unverified { background: #f5f5f5; }
.status-pending { background: #FFF8E1; }
.status-approved { background: #E8F5E9; }
.status-rejected { background: #FFEBEE; }
.status-frozen { background: #ECEFF1; }

.status-icon { font-size: 36px; }
.status-text h3 { font-size: 18px; font-weight: 600; }
.status-text p { font-size: 14px; color: var(--text-secondary); margin-top: 4px; }
.remark { font-size: 13px; font-style: italic; color: var(--text-muted); margin-top: 6px !important; }

.divider { height: 1px; background: var(--border-light); margin: 24px 0; }

.verification-form-section h3, .appeal-section h3 { font-size: 18px; font-weight: 600; margin-bottom: 16px; }

.identity-type-options { display: flex; gap: 12px; }
.identity-option {
  flex: 1; padding: 14px; border: 2px solid var(--border); border-radius: var(--radius);
  text-align: center; cursor: pointer; transition: all 0.2s;
}
.identity-option input { display: none; }
.identity-option.active { border-color: var(--primary); background: var(--primary-light); }
.identity-label { font-size: 15px; font-weight: 500; }

.input-hint { font-size: 12px; color: var(--text-muted); margin-top: 6px; }

.appeal-desc { font-size: 14px; color: var(--text-secondary); margin-bottom: 16px; }

.btn-warning {
  background: var(--secondary);
  color: #fff;
}
.btn-warning:hover { background: #E65100; }

.frozen-notice {
  padding: 20px; background: #FFF3E0; border-radius: var(--radius);
  color: #E65100; font-size: 14px; text-align: center; margin-top: 24px;
}
</style>
