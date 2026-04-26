package com.atguigu.spzx.manager.service.impl;

import com.alibaba.excel.EasyExcel;
import com.atguigu.spzx.common.service.exception.GuiguException;
import com.atguigu.spzx.manager.listener.ExcelListener;
import com.atguigu.spzx.manager.mapper.CategoryMapper;
import com.atguigu.spzx.manager.service.CategoryService;
import com.atguigu.spzx.model.entity.product.Category;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.model.vo.product.CategoryExcelVo;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> findByParentId(Long parentId) {
        List<Category> categoryList = categoryMapper.selectByParentId(parentId);
        if (!CollectionUtils.isEmpty(categoryList)) {

            // 遍历分类的集合，获取每一个分类数据
            categoryList.forEach(item -> {
                int count = categoryMapper.countByParentId(item.getId());
                item.setHasChildren(count > 0);
            });
        }
        return categoryList;
    }

    @Override
    public void exportData(HttpServletResponse response) {
        try {
            // 设置响应结果类型
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("utf-8");

            String fileName = URLEncoder.encode("分类数据", StandardCharsets.UTF_8) + ".xlsx";

            // 设置响应头为下载
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            List<Category> categoryList = categoryMapper.findAll();
            List<CategoryExcelVo> categoryExcelVoList = new ArrayList<>(categoryList.size());

            // 将从数据库中查询到的Category对象转换成CategoryExcelVo对象
            for(Category category : categoryList) {
                CategoryExcelVo categoryExcelVo = new CategoryExcelVo();
                BeanUtils.copyProperties(category, categoryExcelVo, CategoryExcelVo.class);
                categoryExcelVoList.add(categoryExcelVo);
            }
            EasyExcel.write(response.getOutputStream(), CategoryExcelVo.class)
                    .sheet("分类数据").doWrite(categoryExcelVoList);

        } catch (Exception e) {
            e.printStackTrace();
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }

    @Override
    public void importData(MultipartFile file) {
        try {
            // 创建监听器
            ExcelListener<CategoryExcelVo> excelListener = new ExcelListener(categoryMapper);
            EasyExcel.read(file.getInputStream(), CategoryExcelVo.class, excelListener)
                    .sheet().doRead();
        } catch (IOException e) {
            e.printStackTrace();
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }

}
