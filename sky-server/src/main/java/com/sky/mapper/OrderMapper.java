package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param order
     */
    void insert(Orders order);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);


    /**
     * 根据id查询订单
     * @param orderId
     */
    @Select("select * from orders where id = #{orderId}")
    Orders getByOrderId(Long orderId);

    /**
     * 修改订单信息
     * @param orders
     */

    void update(Orders orders);

    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Update("update Orders set status = 6,cancel_reason = #{cancelReason} where id = #{id}")
    void cancel(OrdersCancelDTO ordersCancelDTO);

    @Select("select count(*) from orders where status = #{status}")
    Integer statistics(Integer status);


    @Select("select * from orders where  status = #{status} and order_time< #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLt(Integer status, LocalDateTime orderTime);


    Double sumByMap(Map map);

    Integer sumOrdersByMap(Map map);

    List<Orders> getOrdersByTime(Map map);
}
