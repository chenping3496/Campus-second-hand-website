import request from './request'

export function getUserInfo() {
  return request.get('/user/info')
}

export function getUserInfoById(id) {
  return request.get(`/user/info/${id}`)
}

export function updateUserInfo(data) {
  return request.put('/user/info', data)
}

export function changePassword(data) {
  return request.post('/user/password', data)
}
