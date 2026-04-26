package com.atguigu.spzx.manager.controller;

import com.atguigu.spzx.manager.service.CategoryBrandService;
import com.atguigu.spzx.model.dto.product.CategoryBrandDto;
import com.atguigu.spzx.model.entity.product.Brand;
import com.atguigu.spzx.model.entity.product.CategoryBrand;
import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/admin/product/categoryBrand")
public class CategoryBrandController {

    @Autowired
    private CategoryBrandService categoryBrandService;

    @GetMapping("/{page}/{limit}")
    public Result findByPage(@PathVariable("page") Integer page,
                             @PathVariable("limit") Integer limit,
                             CategoryBrandDto categoryBrandDto) {
        PageInfo<CategoryBrand> pageInfo = categoryBrandService.findByPage(page, limit, categoryBrandDto);
        return Result.build(pageInfo, ResultCodeEnum.SUCCESS);
    }

    @PostMapping("save")
    public Result save(@RequestBody CategoryBrandDto categoryBrandDto) {
        categoryBrandService.save(categoryBrandDto);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @PutMapping("updateById")
    public Result updateById(@RequestBody CategoryBrand categoryBrand) {
        categoryBrandService.updateById(categoryBrand);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @DeleteMapping("/deleteById/{id}")
    public Result deleteById(@PathVariable Long id) {
        categoryBrandService.deleteById(id);
        return Result.build(null, ResultCodeEnum.SUCCESS) ;
    }

    @GetMapping("/findBrandByCategoryId/{categoryId}")
    public Result findBrandByCategoryId(@PathVariable("categoryId") Long id) {
        List<Brand> brandList = categoryBrandService.findBrandByCategoryId(id);
        return Result.build(brandList, ResultCodeEnum.SUCCESS) ;
    }
}
