package com.atguigu.spzx.order.controller;

import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.order.service.StockReservationReconcileService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/order/reconcile")
public class StockReservationReconcileController {

    private static final int MANUAL_RECONCILE_BATCH_SIZE = 500;

    @Autowired
    private StockReservationReconcileService stockReservationReconcileService;

    @Operation(summary = "手动触发秒杀预占对账修复")
    @PostMapping("/auth/stockReservation")
    public Result<Map<String, Integer>> reconcileStockReservation() {
        Map<String, Integer> reconcileResult = stockReservationReconcileService.reconcile(MANUAL_RECONCILE_BATCH_SIZE);
        return Result.<Map<String, Integer>>build(reconcileResult, ResultCodeEnum.SUCCESS);
    }
}
