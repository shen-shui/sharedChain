package com.atguigu.spzx.manager.controller;

import com.atguigu.spzx.manager.service.ProductSpecService;
import com.atguigu.spzx.model.entity.product.ProductSpec;
import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/admin/product/productSpec")
public class ProductSpecController {

    @Autowired
    private ProductSpecService productSpecService;

    @GetMapping("/{page}/{limit}")
    public Result findByPage(@PathVariable("page") Integer page,
                             @PathVariable("limit") Integer limit) {
        PageInfo<ProductSpec> pageInfo = productSpecService.findByPage(page, limit);
        return Result.build(pageInfo, ResultCodeEnum.SUCCESS);
    }

    @PostMapping("/save")
    public Result save(@RequestBody ProductSpec productSpec) {
        productSpecService.save(productSpec);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @PutMapping("/updateById")
    public Result updateById(@RequestBody ProductSpec productSpec) {
        productSpecService.updateById(productSpec);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @DeleteMapping("/deleteById/{id}")
    public Result deleteById(@PathVariable("id") Long id) {
        productSpecService.deleteById(id);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @GetMapping("findAll")
    public Result findAll() {
        List<ProductSpec> psList = productSpecService.findAll();
        return Result.build(psList, ResultCodeEnum.SUCCESS);
    }
}
