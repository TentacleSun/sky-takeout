package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Api("商家订单管理")
@RestController
@RequestMapping("/admin/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping("/conditionSearch")
    @ApiOperation("分页按条件查找订单")
    public Result<PageResult> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("分页查找订单：{}",ordersPageQueryDTO);
        PageResult pageResult = orderService.historyOrders(ordersPageQueryDTO,0);
        return Result.success(pageResult);
    }

    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO){
        log.info("根据订单id取消订单");
        orderService.cancel(ordersCancelDTO);

        return Result.success();
    }

    @GetMapping("/statistics")
    @ApiOperation("统计")
    public Result<OrderStatisticsVO> statistics(){
        log.info("根据订单id取消订单");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();

        return Result.success(orderStatisticsVO);
    }

    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        log.info("根据订单id接单：{}",ordersConfirmDTO);
        orderService.confirm(ordersConfirmDTO.getId());
        return Result.success();
    }
    @GetMapping("/details/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> details(@PathVariable Long id){
        log.info("查询订单详情：{}",id);
        OrderVO orderVO = orderService.orderDeatil(id);
        return Result.success(orderVO);
    }

    @PutMapping("/delivery/{id}")
    @ApiOperation("派送")
    public Result delivery(@PathVariable Long id){
        log.info("根据订单id派送：{}",id);
        orderService.delivery(id);
        return Result.success();
    }

    @PutMapping("/complete/{id}")
    @ApiOperation("完成")
    public Result complete(@PathVariable Long id){
        log.info("根据订单id派送：{}",id);
        orderService.complete(id);
        return Result.success();
    }

    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result rejection(@RequestBody OrdersCancelDTO ordersCancelDTO){
        log.info("根据订单id取消订单");
        orderService.cancel(ordersCancelDTO);

        return Result.success();
    }

}
