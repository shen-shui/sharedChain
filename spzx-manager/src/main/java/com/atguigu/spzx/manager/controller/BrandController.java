package com.atguigu.spzx.manager.controller;

import com.atguigu.spzx.manager.service.BrandService;
import com.atguigu.spzx.model.entity.product.Brand;
import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/admin/product/brand")
public class BrandController {

    @Autowired
    private BrandService brandService;

    @GetMapping("/findByPage/{page}/{limit}")
    public Result findByPage(@PathVariable("page") Integer page,
                             @PathVariable("limit") Integer limit) {
        PageInfo<Brand> pageInfo = brandService.findByPage(page, limit);
        return Result.build(pageInfo, ResultCodeEnum.SUCCESS);
    }

    @PostMapping("/save")
    public Result save(@RequestBody Brand brand) {
        brandService.save(brand);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @PutMapping("/updateById")
    public Result updateById(@RequestBody Brand brand) {
        brandService.updateById(brand);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @DeleteMapping("/deleteById/{id}")
    public Result deleteById(@PathVariable Long id) {
        brandService.deleteById(id);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @GetMapping("/findAll")
    public Result findAll() {
        List<Brand> brandList = brandService.findAll();
        return Result.build(brandList, ResultCodeEnum.SUCCESS);
    }
}
