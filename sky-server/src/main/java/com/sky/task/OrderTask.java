package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;
    /**
     * 处理超时订单
     * 每分钟触发一次
     * */
    //@Scheduled(cron = "1/5 * * * * ? ")
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrders(){
        log.info("定时处理超时订单：{}", LocalDateTime.now());

        List<Orders> list = orderMapper.getByStatusAndOrderTimeLt(Orders.PENDING_PAYMENT,LocalDateTime.now().minusMinutes(15));
        if(list!=null &&list.size()>0){
            list.forEach(orders -> {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            });
        }

    }

    /**
     * 定时处理派送中订单
     * 每分钟触发一次
     * */
    //@Scheduled(cron = "0/5 * * * * ? ")
    @Scheduled(cron = "0 0 1 * * ? ")
    public void processDeliveryOrders(){
        log.info("定时处理派送中订单：{}", LocalDateTime.now());

        List<Orders> list = orderMapper.getByStatusAndOrderTimeLt(Orders.DELIVERY_IN_PROGRESS,LocalDateTime.now());
        if(list!=null &&list.size()>0){
            list.forEach(orders -> {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单运送超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            });
        }

    }
}
