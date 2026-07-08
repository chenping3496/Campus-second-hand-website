import request from './request'

export function getProductList(params) {
  return request.get('/products/list', { params })
}

export function searchProducts(params) {
  return request.get('/products/search', { params })
}

export function getProductDetail(id) {
  return request.get(`/products/detail/${id}`)
}

export function createProduct(data) {
  return request.post('/products', data)
}

export function updateProduct(id, data) {
  return request.put(`/products/${id}`, data)
}

export function deleteProduct(id) {
  return request.delete(`/products/${id}`)
}

export function offShelfProduct(id) {
  return request.post(`/products/${id}/off-shelf`)
}

export function relistProduct(id) {
  return request.post(`/products/${id}/relist`)
}

export function getMyProducts(params) {
  return request.get('/products/my', { params })
}
