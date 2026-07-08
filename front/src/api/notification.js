import request from './request'

export function getNotifications(params) {
  return request.get('/notifications', { params })
}

export function getUnreadNotificationCount() {
  return request.get('/notifications/unread')
}

export function markNotificationRead(id) {
  return request.post(`/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return request.post('/notifications/read-all')
}
