import request from './request'

export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadBase64Image(filename, data) {
  return request.post('/upload/base64', { filename, data })
}
