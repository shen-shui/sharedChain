package com.atguigu.spzx.model.entity.order;

import com.atguigu.spzx.model.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "库存预占记录")
public class StockReservation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "商品skuId")
    private Long skuId;

    @Schema(description = "预占数量")
    private Integer reserveNum;

    @Schema(description = "预占状态：0-已预占，1-已确认，2-已释放")
    private Integer reservationStatus;

    @Schema(description = "预占过期时间")
    private Date expireTime;

    @Schema(description = "确认时间")
    private Date confirmedTime;

    @Schema(description = "释放时间")
    private Date releasedTime;
}
