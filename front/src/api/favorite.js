import request from './request'

export function addFavorite(productId) {
  return request.post(`/favorites/${productId}`)
}

export function removeFavorite(productId) {
  return request.delete(`/favorites/${productId}`)
}

export function getFavorites(params) {
  return request.get('/favorites', { params })
}

export function checkFavorite(productId) {
  return request.get(`/favorites/check/${productId}`)
}
