package com.atguigu.spzx.product.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.spzx.model.entity.product.Category;
import com.atguigu.spzx.product.mapper.CategoryMapper;
import com.atguigu.spzx.product.service.CategoryService;
import com.github.pagehelper.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 更新成使用redis进行缓存
    @Override
    public List<Category> findOneCategory() {
        String categoryListJSON = redisTemplate.opsForValue().get("category:one");

        // 尝试不同的空判断
        if (categoryListJSON != null && !categoryListJSON.trim().isEmpty()) {
            return JSON.parseArray(categoryListJSON, Category.class);
        }

        List<Category> categoryList = categoryMapper.findOneCategory();
        redisTemplate.opsForValue().set("category:one", JSON.toJSONString(categoryList), 7, TimeUnit.DAYS);
        return categoryList;
    }

    @Cacheable(value = "category" , key = "'all'")
    @Override
    public List<Category> findCategoryTree() {
        // 查找所有分类
        List<Category> categoryList = categoryMapper.findAll();

        // 查找所有一级分类
        List<Category> oneCategoryList = categoryList.stream()
                .filter(item -> item.getParentId() == 0)
                .toList();

        // 查找所有二级分类
        oneCategoryList.forEach(oneCategory -> {
            List<Category> twoCategoryList = categoryList.stream()
                    .filter(item -> item.getParentId() == oneCategory.getId())
                    .toList();

            oneCategory.setChildren(twoCategoryList);

            // 查找所有的三级分类
            twoCategoryList.forEach(twoCategory -> {
                List<Category> threeCategoryList = categoryList.stream()
                        .filter(item -> item.getParentId() == twoCategory.getId())
                        .toList();

                twoCategory.setChildren(threeCategoryList);
            });
        });

        return oneCategoryList;
    }
}
