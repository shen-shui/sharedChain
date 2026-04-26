package com.atguigu.spzx.feign.product;

import com.atguigu.spzx.model.dto.product.SkuSaleDto;
import com.atguigu.spzx.model.entity.product.ProductSku;
import com.atguigu.spzx.model.vo.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(value = "service-product")
public interface ProductFeignClient {

    @GetMapping("/api/product/getBySkuId/{skuId}")
    ProductSku getBySkuId(@PathVariable("skuId") Long skuId);

    @PostMapping("/api/product/updateSkuSaleNum")
    Boolean updateSkuSaleNum(@RequestBody List<SkuSaleDto> skuSaleDtoList);

    @PostMapping("/api/product/inner/deductStock/{skuId}/{skuNum}")
    Result<Boolean> deductStock(@PathVariable("skuId") Long skuId,
                                @PathVariable("skuNum") Integer skuNum);
}
