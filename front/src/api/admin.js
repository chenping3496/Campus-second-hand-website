import request from './request'

// User management
export function getAdminUsers(params) {
  return request.get('/admin/users', { params })
}

export function banUser(id) {
  return request.post(`/admin/users/${id}/ban`)
}

export function unbanUser(id) {
  return request.post(`/admin/users/${id}/unban`)
}

// Product management
export function getAdminProducts(params) {
  return request.get('/admin/products', { params })
}

export function approveProduct(id) {
  return request.post(`/admin/products/${id}/approve`)
}

export function rejectProduct(id, reason) {
  return request.post(`/admin/products/${id}/reject`, { reason })
}

export function forceOffShelf(id, reason) {
  return request.post(`/admin/products/${id}/force-off-shelf`, { reason })
}

// Category management
export function getAdminCategories() {
  return request.get('/admin/categories')
}

export function createCategory(data) {
  return request.post('/admin/categories', data)
}

export function updateCategory(id, data) {
  return request.put(`/admin/categories/${id}`, data)
}

export function deleteCategory(id) {
  return request.delete(`/admin/categories/${id}`)
}

// Order management
export function getAdminOrders(params) {
  return request.get('/admin/orders', { params })
}

// Statistics
export function getStatistics() {
  return request.get('/admin/statistics')
}
