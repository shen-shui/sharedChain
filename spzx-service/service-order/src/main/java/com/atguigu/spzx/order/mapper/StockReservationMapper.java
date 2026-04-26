package com.atguigu.spzx.order.mapper;

import com.atguigu.spzx.model.entity.order.StockReservation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockReservationMapper {

    void save(StockReservation stockReservation);

    StockReservation getByOrderNo(String orderNo);

    int updateStatusByOrderNo(String orderNo, Integer fromStatus, Integer toStatus);
}
