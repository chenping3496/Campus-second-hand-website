package com.example.backend.config;

import com.example.backend.entity.*;
import com.example.backend.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initCategories();
        initUsers();
        initProducts();
    }

    private void initCategories() {
        if (categoryMapper.selectCount(null) > 0) {
            return;
        }

        List<String[]> categories = Arrays.asList(
                new String[]{"教材书籍", "📚"},
                new String[]{"电子产品", "📱"},
                new String[]{"生活用品", "🏠"},
                new String[]{"服装鞋帽", "👕"},
                new String[]{"运动器材", "⚽"},
                new String[]{"美妆护肤", "💄"},
                new String[]{"食品零食", "🍔"},
                new String[]{"其他", "📦"}
        );

        int order = 1;
        for (String[] cat : categories) {
            Category category = new Category();
            category.setName(cat[0]);
            category.setIcon(cat[1]);
            category.setSortOrder(order++);
            category.setEnabled(true);
            categoryMapper.insert(category);
        }
    }

    private void initUsers() {
        if (userMapper.selectCount(null) > 0) {
            return;
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setNickname("管理员");
        admin.setRole(User.UserRole.ADMIN);
        admin.setStatus(User.UserStatus.ACTIVE);
        admin.setAvatar("/images/avatars/admin.png");
        userMapper.insert(admin);

        String[] nicknames = {"小明", "小红", "小刚", "小丽", "小强", "小芳", "小伟", "小娟", "小军", "小燕"};
        for (int i = 1; i <= 10; i++) {
            User user = new User();
            user.setUsername("user" + i);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setNickname(nicknames[i - 1]);
            user.setRole(User.UserRole.USER);
            user.setStatus(User.UserStatus.ACTIVE);
            user.setPhone("1380000000" + i);
            user.setEmail("user" + i + "@campus.edu");
            user.setStudentId("2024000" + String.format("%03d", i));
            user.setDormitory("学生公寓" + ((i % 5) + 1) + "号楼" + (100 + i) + "室");
            user.setAvatar("/images/avatars/user" + i + ".png");
            userMapper.insert(user);
        }
    }

    private void initProducts() {
        if (productMapper.selectCount(null) > 0) {
            return;
        }

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getRole, User.UserRole.USER);
        List<User> users = userMapper.selectList(userWrapper);
        List<Category> categories = categoryMapper.selectList(null);

        if (users.isEmpty() || categories.isEmpty()) {
            return;
        }

        Object[][] products = {
                {"高等数学第七版上下册", "同济大学出版，九成新，有少量笔记", new BigDecimal("35.00"), new BigDecimal("89.00"), "教材书籍", "/images/products/math_book.png"},
                {"大学英语四级词汇书", "全新未拆封，送配套练习册", new BigDecimal("15.00"), new BigDecimal("39.00"), "教材书籍", "/images/products/english_book.png"},
                {"计算机组成原理教材", "唐朔飞版，八成新", new BigDecimal("25.00"), new BigDecimal("59.00"), "教材书籍", "/images/products/computer_book.png"},

                {"iPhone 12 128G", "国行正品，电池健康89%，无磕碰", new BigDecimal("2800.00"), new BigDecimal("5999.00"), "电子产品", "/images/products/iphone12.png"},
                {"小米平板5", "使用半年，配件齐全，送保护壳", new BigDecimal("1200.00"), new BigDecimal("1999.00"), "电子产品", "/images/products/xiaomi_pad.png"},
                {"罗技G502游戏鼠标", "九成新，手感极佳", new BigDecimal("180.00"), new BigDecimal("399.00"), "电子产品", "/images/products/mouse.png"},

                {"宜家台灯", "LED护眼灯，三档调光", new BigDecimal("45.00"), new BigDecimal("99.00"), "生活用品", "/images/products/lamp.png"},
                {"小型电风扇", "USB供电，静音设计", new BigDecimal("25.00"), new BigDecimal("59.00"), "生活用品", "/images/products/fan.png"},
                {"收纳箱三件套", "可折叠，节省空间", new BigDecimal("30.00"), new BigDecimal("79.00"), "生活用品", "/images/products/storage_box.png"},

                {"Nike运动鞋42码", "正品，穿过几次，九成新", new BigDecimal("280.00"), new BigDecimal("699.00"), "服装鞋帽", "/images/products/nike_shoes.png"},
                {"优衣库羽绒服M码", "去年款，保暖效果好", new BigDecimal("150.00"), new BigDecimal("499.00"), "服装鞋帽", "/images/products/down_jacket.png"},

                {"羽毛球拍一对", "尤尼克斯入门款，送球", new BigDecimal("80.00"), new BigDecimal("199.00"), "运动器材", "/images/products/badminton.png"},
                {"瑜伽垫", "加厚防滑，送收纳袋", new BigDecimal("35.00"), new BigDecimal("89.00"), "运动器材", "/images/products/yoga_mat.png"},

                {"兰蔻小黑瓶精华", "30ml，用了一半", new BigDecimal("200.00"), new BigDecimal("760.00"), "美妆护肤", "/images/products/lancome.png"},
                {"完美日记眼影盘", "全新未拆", new BigDecimal("50.00"), new BigDecimal("129.00"), "美妆护肤", "/images/products/eyeshadow.png"}
        };

        for (int i = 0; i < products.length; i++) {
            Object[] p = products[i];
            Product product = new Product();
            product.setTitle((String) p[0]);
            product.setDescription((String) p[1]);
            product.setPrice((BigDecimal) p[2]);
            product.setOriginalPrice((BigDecimal) p[3]);

            String categoryName = (String) p[4];
            Category category = categories.stream()
                    .filter(c -> c.getName().equals(categoryName))
                    .findFirst()
                    .orElse(categories.get(0));
            product.setCategoryId(category.getId());

            User seller = users.get(i % users.size());
            product.setSellerId(seller.getId());

            product.setImages((String) p[5]);
            product.setStatus(Product.ProductStatus.ON_SALE);
            product.setViewCount((int) (Math.random() * 100));

            productMapper.insert(product);
        }
    }
}
