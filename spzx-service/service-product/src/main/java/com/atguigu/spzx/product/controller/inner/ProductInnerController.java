package com.atguigu.spzx.product.controller.inner;

import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product/inner")
public class ProductInnerController {

    @Autowired
    private ProductService productService;

    @PostMapping("/deductStock/{skuId}/{skuNum}")
    public Result<Boolean> deductStock(@PathVariable Long skuId,
                                       @PathVariable Integer skuNum) {
        Boolean success = productService.deductStock(skuId, skuNum);
        if (!Boolean.TRUE.equals(success)) {
            return Result.<Boolean>build(false, ResultCodeEnum.STOCK_LESS);
        }
        return Result.<Boolean>build(true, ResultCodeEnum.SUCCESS);
    }

    @PostMapping("/restoreStock/{skuId}/{skuNum}")
    public Result<Boolean> restoreStock(@PathVariable Long skuId,
                                        @PathVariable Integer skuNum) {
        Boolean success = productService.restoreStock(skuId, skuNum);
        if (!Boolean.TRUE.equals(success)) {
            return Result.<Boolean>build(false, ResultCodeEnum.DATA_ERROR);
        }
        return Result.<Boolean>build(true, ResultCodeEnum.SUCCESS);
    }
}

