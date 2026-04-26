# 秒杀链路优化工作总结（PR1-PR10）

本文档用于汇总本轮秒杀流程优化的实施结果，方便快速了解：

- 每个 PR 做了什么
- 每个 PR 解决了哪些问题
- 当前系统已经具备哪些能力
- 下一步还可以继续做哪些工作（第11-13块）

---

## 1. 迭代总览

本次改造围绕秒杀场景的核心目标展开：

1. 高并发入口抗压（Redis 预扣 + MQ 削峰）
2. 订单与库存状态最终一致（预占状态机 + 补偿 + 对账）
3. 并发冲突可控（状态 CAS）
4. 异常路径可追踪、可重试、可兜底（日志、重试、DLQ）

---

## 2. PR 明细（PR1 - PR10）

### PR1: `feat: add seckill stock reservation foundation`
- 主要工作
  - 新增库存预占实体 `StockReservation`
  - 新增预占 Mapper 与 XML
  - 新增 `stock_reservation` 建表 SQL
  - 秒杀异步建单成功后写入 `RESERVED` 预占记录
- 解决问题
  - 解决“只有 Redis 预扣，没有持久化预占凭证”的问题
  - 为后续支付确认/超时释放提供状态机基础

### PR2: `feat: harden seckill entry with lua pre-deduct`
- 主要工作
  - 秒杀入口预扣改为 Lua 原子脚本
  - 用户防重 key 增加活动维度
  - 用户防重 key 增加 TTL
- 解决问题
  - 解决 `check-then-decrement` 并发窗口风险
  - 避免防重 key 长期残留和活动间互相污染

### PR3: `feat: add reservation transitions for pay and timeout`
- 主要工作
  - 支付成功：`RESERVED -> CONFIRMED`
  - 超时关单：`RESERVED -> RELEASED`，释放成功才回补 Redis
  - 预占 Mapper 拆分显式状态流转方法
- 解决问题
  - 建立预占状态机闭环
  - 避免重复释放导致重复回补库存

### PR4: `feat: add stock reservation compensation task`
- 主要工作
  - 启用定时任务
  - 新增过期预占补偿任务：扫描过期 `RESERVED`，执行释放+回补
  - 新增补偿频率配置
- 解决问题
  - 兜底 MQ 丢消息、消费者异常等导致的未释放预占
  - 提升最终一致性收敛能力

### PR5: `feat: add stock reservation reconcile flow`
- 主要工作
  - 新增对账服务与定时对账任务
  - 新增手动触发对账接口
  - 扫描“订单已终态但预占仍 RESERVED”的异常并修复
- 解决问题
  - 解决状态漂移（订单状态与预占状态不一致）
  - 提供线上人工一键修复入口

### PR6: `chore: add observability logs for seckill flow`
- 主要工作
  - 在秒杀入口、异步建单、支付确认、超时释放、补偿任务增加关键日志
  - 统一关键字段（orderNo/userId/skuId 等）输出
- 解决问题
  - 提升问题定位效率
  - 增强链路可观测性和审计可追踪性

### PR7: `refactor: make seckill parameters configurable`
- 主要工作
  - 新增 `SeckillProperties`
  - 活动 ID、防重 TTL、补偿批次、对账批次等改为配置驱动
  - 扩展 `application-dev.yml` 参数
- 解决问题
  - 消除硬编码，便于不同环境调参
  - 降低后续运维与灰度成本

### PR8: `feat: close seckill mysql stock ledger on payment`
- 主要工作
  - 支付成功消费中引入 MySQL 最终扣库存（调用 `deductStock`）
  - 增加预占状态保护（已释放不再走支付扣减）
- 解决问题
  - 解决“只在 Redis 预扣但 MySQL 未最终记账”的缺口
  - 建立支付成功后库存最终账本闭环

### PR9: `feat: add CAS order status transitions for pay-timeout race`
- 主要工作
  - 新增 `updateStatusByOrderNo(orderNo, fromStatus, toStatus)` 条件更新
  - 支付与超时均改为 CAS 状态流转
- 解决问题
  - 解决支付成功与超时关单并发覆盖问题
  - 保证终态互斥：只有一个流转成功

### PR10: `feat: add bounded mq retries and DLQ fallback`
- 主要工作
  - 三个关键消费者设置 `maxReconsumeTimes = 5`
  - 部分可重试场景改为抛异常触发 RocketMQ 重试
  - 依赖 RocketMQ 超重试后自动进入 DLQ
  - 清理重复配置项
- 解决问题
  - 解决“临时异常被直接吞掉”的风险
  - 建立最小重试+死信兜底能力

---

## 3. 当前已经具备的能力

经过 PR1-PR10，当前秒杀链路已具备：

1. Redis 原子预扣与入口防重
2. 预占状态机（`RESERVED/CONFIRMED/RELEASED`）
3. 超时释放与 Redis 回补
4. 定时补偿与定时/手动对账修复
5. 支付后 MySQL 最终库存记账
6. 支付/超时并发 CAS 互斥
7. 关键链路可观测日志
8. 有界 MQ 重试与 DLQ 自动兜底
9. 核心参数配置化

---

## 4. 后续建议（第11-13块）

### 第11块（优先）DLQ 人工补偿入口
- 建议内容
  - 新增 DLQ 消息处理接口（按 `orderNo` 手动重放/修复）
  - 提供“只重放一次”“仅修复状态不重放消息”等模式
- 价值
  - 降低线上死信处理的人肉成本
  - 把 DLQ 从“堆积告警”变成“可操作闭环”

### 第12块 监控与告警指标化
- 建议内容
  - 将关键日志沉淀为指标：重试次数、DLQ 数量、预占释放失败数、对账修复数等
  - 配置阈值告警和趋势看板
- 价值
  - 实时感知风险并提前干预
  - 把“事后排查”转为“事中告警”

### 第13块 支付扣库存幂等加固
- 建议内容
  - 增加支付扣库存幂等标记（例如按 `orderNo` 记录扣减状态）
  - 避免极端重复消息/重复回调导致重复扣减
- 价值
  - 进一步收敛资金与库存一致性风险
  - 提升支付链路稳定性

---

## 5. 落地建议顺序

推荐按以下顺序继续：

1. 第11块：DLQ 人工补偿入口
2. 第13块：支付扣库存幂等加固
3. 第12块：监控告警指标化

原因：
- 第11/13直接影响异常处置与资金库存安全，优先级更高
- 第12属于治理增强，依赖前面能力更稳定后效果更好

---

## 6. 备注

- 本文档聚焦秒杀库存与订单一致性链路，不覆盖所有业务模块。
- 线上发布建议继续采用“灰度开关 + 分批放量 + 可回滚”策略。
