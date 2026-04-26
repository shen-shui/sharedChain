CREATE TABLE IF NOT EXISTS stock_reservation (
    id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户id',
    sku_id BIGINT NOT NULL COMMENT '商品skuId',
    reserve_num INT NOT NULL COMMENT '预占数量',
    reservation_status TINYINT NOT NULL COMMENT '状态：0-已预占，1-已确认，2-已释放',
    expire_time DATETIME DEFAULT NULL COMMENT '预占过期时间',
    confirmed_time DATETIME DEFAULT NULL COMMENT '确认时间',
    released_time DATETIME DEFAULT NULL COMMENT '释放时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_sku_status (sku_id, reservation_status),
    KEY idx_expire_status (expire_time, reservation_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀库存预占记录表';
