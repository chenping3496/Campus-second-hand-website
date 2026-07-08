import request from './request'

export function getCategories() {
  return request.get('/categories/list')
}
