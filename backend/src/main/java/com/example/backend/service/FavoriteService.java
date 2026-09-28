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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
     * 在当前事务提交成功后执行 Redis 操作；无事务上下文时直接执行。
     * 关键：afterCommit 仅在事务成功提交后触发，事务回滚则不执行，
     * 因此 Redis 永远不会领先于 DB（Redis 最多偏少，由 getFavoriteCount 冷启动回源自愈）。
     */
    private void executeAfterCommit(Runnable redisAction) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redisAction.run();
                }
            });
        } else {
            redisAction.run();
        }
    }

    /**
     * 收藏：DB 作为真相源先写入，Redis 仅作计数缓存，在事务提交后（afterCommit）执行，
     * 避免「Redis 先写、DB 回滚」导致 Redis 计数虚高且无法自愈。
     *
     * 防重：依赖 DB 唯一约束 favorites(user_id, product_id)（见 sql/V2__favorites_unique_index.sql），
     * 并发重复收藏由 DuplicateKeyException 兜底。
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

        // 1. 先写 DB（真相源）；事务回滚则 Redis 不会被触碰
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            return Result.error("已收藏该商品");
        }

        // 2. DB 事务提交成功后再更新 Redis；事务回滚则 afterCommit 不会被调用
        executeAfterCommit(() -> {
            // favoriteAddScript：SISMEMBER 守卫 + SADD + INCR；
            // 已在 Redis 中（历史残留）则返回 -1、不重复计数，恰好也是期望行为。
            List<String> keys = List.of(favCountKey(productId), userFavKey(userId));
            stringRedisTemplate.execute(favoriteAddScript, keys, productId.toString());
        });

        return Result.success();
    }

    /**
     * 取消收藏：同 addFavorite，DB 先删、Redis 在事务提交后再撤销计数，保证 Redis 永不领先于 DB。
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

        // 1. 先删 DB（幂等删除，影响 0 行也无妨）
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId);
        favoriteMapper.delete(wrapper);

        // 2. DB 提交后再撤销 Redis；favoriteRemoveScript 内 SISMEMBER==0 时直接返回不 DECR，
        //    故 Redis 偏少不会进一步恶化，可由 getFavoriteCount 冷启动回源自愈。
        executeAfterCommit(() -> {
            List<String> keys = List.of(favCountKey(productId), userFavKey(userId));
            stringRedisTemplate.execute(favoriteRemoveScript, keys, productId.toString());
        });

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
