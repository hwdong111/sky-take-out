package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类，定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理订单超时的方法
     */
    @Scheduled(cron = "0 * * * * ?")//每分钟一次
    //@Scheduled(cron = "1/6 * * * * ?")//每6秒一次
    public void processTimeoutOrder() {
        log.info("处理订单超时--{}", LocalDateTime.now());
        List<Orders> ordersList = orderMapper.getByStatusOrderTimeLT(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));

        if (ordersList != null && ordersList.size() > 0) {
            for (Orders orders : ordersList) {
                //将订单状态改为取消
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时未支付,自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }

    }

    /**
     * 定时处理一直处于派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")//每天凌晨1点处理一次
    //@Scheduled(cron = "0/6 * * * * ?")//每6秒一次
    public void processDeliveredOrder() {
        log.info("定时处理一直处于派送中的订单:{}", LocalDateTime.now());
        List<Orders> ordersList = orderMapper.getByStatusOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusMinutes(-60));

        if (ordersList != null && ordersList.size() > 0){
            for (Orders orders : ordersList) {
                //将订单状态改为已完成
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }

    }
}
