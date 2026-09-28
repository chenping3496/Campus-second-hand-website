-- 迁移：给 favorites(user_id, product_id) 加唯一索引
-- 目的：并发收藏时由 DB 唯一约束防重，第二个 insert 抛 DuplicateKeyException，
--       被 FavoriteService.addFavorite 的 catch 兜底返回"已收藏该商品"。
--
-- 执行：mysql -u root -p ershou < sql/V2__favorites_unique_index.sql
--      （若已存在重复行，先清理：DELETE f1 FROM favorites f1 JOIN favorites f2
--       ON f1.id > f2.id AND f1.user_id=f2.user_id AND f1.product_id=f2.product_id;）

USE ershou;

ALTER TABLE favorites
    ADD UNIQUE KEY uk_user_product (user_id, product_id);
