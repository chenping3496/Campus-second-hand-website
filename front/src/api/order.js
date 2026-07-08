import request from './request'

export function createOrder(data) {
  return request.post('/orders', data)
}

export function getBuyerOrders(params) {
  return request.get('/orders/bought', { params })
}

export function getSellerOrders(params) {
  return request.get('/orders/sold', { params })
}

export function getOrderDetail(id) {
  return request.get(`/orders/${id}`)
}

export function shipOrder(id) {
  return request.post(`/orders/${id}/ship`)
}

export function confirmReceive(id) {
  return request.post(`/orders/${id}/confirm`)
}

export function cancelOrder(id, reason) {
  return request.post(`/orders/${id}/cancel`, { reason })
}
