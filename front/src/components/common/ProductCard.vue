<template>
  <router-link :to="`/product/${product.id}`" class="product-card">
    <div class="product-image-wrapper">
      <img
        v-if="product.images && product.images.length"
        :src="product.images[0]"
        :alt="product.title"
        class="product-image"
        loading="lazy"
      />
      <div v-else class="product-image-placeholder">
        <span>暂无图片</span>
      </div>
      <span v-if="product.status === 'SOLD'" class="sold-badge">已售出</span>
      <span v-if="product.status === 'OFF_SHELF'" class="off-badge">已下架</span>
    </div>
    <div class="product-info">
      <h3 class="product-title">{{ product.title }}</h3>
      <p class="product-desc" v-if="product.description">{{ product.description }}</p>
      <div class="product-meta">
        <span class="product-price">
          <span class="price-unit">¥</span>{{ product.price }}
        </span>
        <span v-if="product.originalPrice" class="price-original">¥{{ product.originalPrice }}</span>
      </div>
      <div class="product-bottom">
        <div class="seller-info">
          <img v-if="product.sellerAvatar" :src="product.sellerAvatar" class="seller-avatar" />
          <span v-else class="seller-avatar-placeholder">👤</span>
          <span class="seller-name">{{ product.sellerName || '匿名用户' }}</span>
        </div>
        <div class="product-stats">
          <span class="view-count">👁 {{ product.viewCount || 0 }}</span>
        </div>
      </div>
    </div>
  </router-link>
</template>

<script setup>
defineProps({
  product: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.product-card {
  display: block;
  background: var(--bg-white);
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow);
  transition: all 0.3s;
  cursor: pointer;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
}

.product-image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 100%;
  background: #f0f0f0;
  overflow: hidden;
}

.product-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-image-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 14px;
}

.sold-badge, .off-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.sold-badge { background: rgba(0,0,0,0.6); }
.off-badge { background: rgba(244,67,54,0.8); }

.product-info {
  padding: 12px;
}

.product-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-desc {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-meta {
  margin-top: 8px;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.product-price {
  font-size: 18px;
  color: var(--danger);
  font-weight: 600;
}

.product-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.seller-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
}

.seller-avatar-placeholder {
  font-size: 14px;
}

.seller-name {
  font-size: 12px;
  color: var(--text-secondary);
}

.view-count {
  font-size: 12px;
  color: var(--text-muted);
}
</style>
