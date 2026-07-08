import request from './request'

// User-side
export function submitComplaint(data) {
  return request.post('/complaints', data)
}

export function getMyComplaints(params) {
  return request.get('/complaints/my', { params })
}

// Admin-side
export function getComplaintList(params) {
  return request.get('/admin/complaints', { params })
}

export function markProcessing(id) {
  return request.post(`/admin/complaints/${id}/processing`)
}

export function resolveComplaint(id, response) {
  return request.post(`/admin/complaints/${id}/resolve`, { response })
}
