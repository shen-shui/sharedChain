package com.atguigu.spzx.order.mapper;

import com.atguigu.spzx.model.entity.order.StockReservation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StockReservationMapper {

    void save(StockReservation stockReservation);

    StockReservation getByOrderNo(String orderNo);

    int confirmByOrderNo(String orderNo, Integer fromStatus);

    int releaseByOrderNo(String orderNo, Integer fromStatus);

    List<StockReservation> findExpiredReserved(int limit);
}
