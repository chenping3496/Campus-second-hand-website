package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.Favorite;
import com.example.backend.entity.Product;
import com.example.backend.entity.User;
import com.example.backend.mapper.FavoriteMapper;
import com.example.backend.mapper.ProductMapper;
import com.example.backend.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DefaultRedisScript<Long> favoriteAddScript;

    @Autowired
    private DefaultRedisScript<Long> favoriteRemoveScript;

    private static String favCountKey(Long productId) {
        return "product:fav:" + productId;
    }

    private static String userFavKey(Long userId) {
        return "user:fav:" + userId;
    }

    /**
     * 收藏：Redis + Lua 原子执行「资格判断（是否已收藏）+ 数据更新（用户集合加入、商品计数 +1）」，
     * 防止并发请求重复收藏、计数丢失；Redis 守卫通过后再落 DB 做持久化。
     */
    @Transactional
    public Result<Void> addFavorite(Long userId, Long productId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted()) {
            return Result.error("商品不存在");
        }

        List<String> keys = List.of(favCountKey(productId), userFavKey(userId));
        Long ret = stringRedisTemplate.execute(favoriteAddScript, keys, productId.toString());
        if (ret == null || ret == -1L) {
            // 已收藏（资格判断失败）——并发场景下只有第一个请求能走到这里
            return Result.error("已收藏该商品");
        }

        // Redis 已作为原子守卫，DB 仅做落盘；偶发 DB 失败由 removeFavorite 幂等自愈
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favoriteMapper.insert(favorite);

        return Result.success();
    }

    /**
     * 取消收藏：同上，Lua 原子执行资格判断 + 数据更新；DB 做幂等删除。
     */
    @Transactional
    public Result<Void> removeFavorite(Long userId, Long productId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        Product product = productMapper.selectById(productId);
        if (product == null) {
            return Result.error("商品不存在");
        }

        List<String> keys = List.of(favCountKey(productId), userFavKey(userId));
        stringRedisTemplate.execute(favoriteRemoveScript, keys, productId.toString());

        // DB 幂等删除（无论 Redis 是否命中，保证持久层一致）
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId);
        favoriteMapper.delete(wrapper);

        return Result.success();
    }

    /**
     * 商品收藏数：Redis 热点数据，冷启动时回源 DB 并回填。
     */
    public long getFavoriteCount(Long productId) {
        String val = stringRedisTemplate.opsForValue().get(favCountKey(productId));
        if (val != null) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException ignored) {
                // 序列异常则回源
            }
        }
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getProductId, productId);
        long count = favoriteMapper.selectCount(wrapper);
        stringRedisTemplate.opsForValue().set(favCountKey(productId), String.valueOf(count));
        return count;
    }

    public PageResult<ProductDTO> getFavorites(Long userId, int page, int size) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return PageResult.of(List.of(), 0, page, size);
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).orderByDesc(Favorite::getCreatedAt);
        Page<Favorite> mpPage = favoriteMapper.selectPage(new Page<>(page + 1, size), wrapper);

        List<ProductDTO> list = mpPage.getRecords().stream()
                .filter(f -> {
                    Product p = productMapper.selectById(f.getProductId());
                    return p != null && !p.getDeleted();
                })
                .map(f -> {
                    Product p = productMapper.selectById(f.getProductId());
                    ProductDTO dto = ProductDTO.fromEntity(p, null, null);
                    dto.setIsFavorited(true);
                    dto.setFavoriteCount(getFavoriteCount(f.getProductId()));
                    return dto;
                })
                .toList();

        return PageResult.of(list, mpPage.getTotal(), page, size);
    }

    public boolean isFavorited(Long userId, Long productId) {
        User user = userMapper.selectById(userId);
        Product product = productMapper.selectById(productId);
        if (user == null || product == null) {
            return false;
        }
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }
}
