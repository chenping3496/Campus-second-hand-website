import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { guest: true }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/ProductDetail.vue')
  },
  {
    path: '/publish',
    name: 'PublishProduct',
    component: () => import('@/views/PublishProduct.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/edit-product/:id',
    name: 'EditProduct',
    component: () => import('@/views/PublishProduct.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/my-products',
    name: 'MyProducts',
    component: () => import('@/views/MyProducts.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: () => import('@/views/Favorites.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('@/views/Orders.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('@/views/OrderDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/chat',
    name: 'ChatList',
    component: () => import('@/views/ChatList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/chat/:id',
    name: 'ChatDetail',
    component: () => import('@/views/ChatDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/Notifications.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'UserProfile',
    component: () => import('@/views/UserProfile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/Dashboard.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('@/views/admin/UserManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/products',
    name: 'AdminProducts',
    component: () => import('@/views/admin/ProductManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/categories',
    name: 'AdminCategories',
    component: () => import('@/views/admin/CategoryManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/orders',
    name: 'AdminOrders',
    component: () => import('@/views/admin/OrderManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

const adminRouteNames = ['AdminDashboard', 'AdminUsers', 'AdminProducts', 'AdminCategories', 'AdminOrders']
const publicRouteNames = ['Login', 'Register', 'NotFound']

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
  const isAdmin = userInfo?.role === 'ADMIN'

  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin && !isAdmin) {
    next({ name: 'Home' })
    return
  }

  // Admin users can only access admin pages and public pages
  if (isAdmin && token && !adminRouteNames.includes(to.name) && !publicRouteNames.includes(to.name)) {
    next({ name: 'AdminDashboard' })
    return
  }

  if (to.meta.guest && token) {
    next(isAdmin ? { name: 'AdminDashboard' } : { name: 'Home' })
    return
  }

  next()
})

export default router
