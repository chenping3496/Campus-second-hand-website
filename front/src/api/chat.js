import request from './request'

export function getConversations(params) {
  return request.get('/chat/conversations', { params })
}

export function getOrCreateConversation(data) {
  return request.post('/chat/conversations', data)
}

export function getConversation(id) {
  return request.get(`/chat/conversations/${id}`)
}

export function getMessages(id, params) {
  return request.get(`/chat/conversations/${id}/messages`, { params })
}

export function sendMessage(id, data) {
  return request.post(`/chat/conversations/${id}/messages`, data)
}

export function markAsRead(id) {
  return request.post(`/chat/conversations/${id}/read`)
}

export function getUnreadCount() {
  return request.get('/chat/unread')
}
