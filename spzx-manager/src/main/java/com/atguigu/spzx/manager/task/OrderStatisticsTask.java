package com.atguigu.spzx.manager.task;

import cn.hutool.core.date.DateUtil;
import com.atguigu.spzx.manager.mapper.OrderInfoMapper;
import com.atguigu.spzx.manager.mapper.OrderStatisticsMapper;
import com.atguigu.spzx.model.entity.order.OrderStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class OrderStatisticsTask {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderStatisticsMapper orderStatisticsMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    private void orderTotalAmountStatistics() {
        // 获取昨天的日期
        String date = DateUtil.offsetDay(new Date(), -1).toString("yyyy-MM-dd");

        // 通过指定的日期获取改日交易总金额
        OrderStatistics orderStatistics = orderInfoMapper.selectOrderStatistics(date);
        if (orderStatistics != null) {
            // 将改日交易总金额插入到orderStatistics表中
            orderStatisticsMapper.insert(orderStatistics);
        }
    }
}
