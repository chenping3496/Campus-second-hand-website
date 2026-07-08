import request from './request'

// User-side
export function getVerificationStatus() {
  return request.get('/verification/status')
}

export function submitVerification(data) {
  return request.post('/verification/submit', data)
}

export function submitAppeal(data) {
  return request.post('/verification/appeal', data)
}

// Admin-side
export function getVerificationList(params) {
  return request.get('/admin/verifications/list', { params })
}

export function approveVerification(userId) {
  return request.post(`/admin/verifications/${userId}/approve`)
}

export function rejectVerification(userId, reason) {
  return request.post(`/admin/verifications/${userId}/reject`, { reason })
}

export function freezeVerification(userId) {
  return request.post(`/admin/verifications/${userId}/freeze`)
}

export function unfreezeVerification(userId) {
  return request.post(`/admin/verifications/${userId}/unfreeze`)
}

export function reviewVerification(userId) {
  return request.post(`/admin/verifications/${userId}/review`)
}

export function getAppeals(params) {
  return request.get('/admin/verifications/appeals', { params })
}

export function handleAppeal(id, data) {
  return request.post(`/admin/verifications/appeals/${id}/handle`, data)
}
