package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;
    //统计营业额
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end){
        //所有的日期的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while(!end.equals(begin)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        //查询当日的已完成订单
        for(LocalDate localDate : dateList){
            LocalDateTime beginTime = LocalDateTime.of(localDate,LocalTime.MIN),
                            endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnoverList.add(turnover==null ? 0.0 : turnover);

        }
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end){

        //所有的日期的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while(!end.equals(begin)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> newUserList =new ArrayList<>(), totalUserList = new ArrayList<>();
        for(LocalDate localDate : dateList){
            LocalDateTime beginTime = LocalDateTime.of(localDate,LocalTime.MIN),
                            endTime = LocalDateTime.of(localDate, LocalTime.MAX);
            Map map =new HashMap();
            map.put("end",endTime);
            Integer totalUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);

            map.put("begin",beginTime);
            Integer newUser = userMapper.countByMap(map);
            newUserList.add(newUser);
        }
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();
    }

    @Transactional
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end){

        //所有的日期的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while(!end.equals(begin)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> validOrdersList = new ArrayList<>(), totalOrdersList = new ArrayList<>();
        //遍历集合，查询每天的有效订单
        for(LocalDate localDate : dateList){
            LocalDateTime beginTime = LocalDateTime.of(localDate,LocalTime.MIN),
                            endTime = LocalDateTime.of(localDate, LocalTime.MAX);
            Integer validOrder = getOrderCount(beginTime,endTime,Orders.COMPLETED);
            Integer totalOrder = getOrderCount(beginTime,endTime,null);
            if(validOrder!=null) validOrdersList.add(validOrder);
            if(totalOrder!=null) totalOrdersList.add(totalOrder);
        }

        //计算订单总量
        Integer totalOrderCount = totalOrdersList.stream().reduce(Integer::sum).get();
        Integer validOrderCount = validOrdersList.stream().reduce(Integer::sum).get();
        //完成率
        Double orderValidateRate = totalOrderCount == 0 ? 0.0 : validOrderCount.doubleValue()/totalOrderCount.doubleValue();

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderValidateRate)
                .orderCountList(StringUtils.join(totalOrdersList,","))
                .validOrderCountList(StringUtils.join(validOrdersList,","))
                .build();
    }

    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end){

        if(end.isBefore(begin)) return null;
        //获得时间条件
        Map map = new HashMap();
        map.put("status",Orders.COMPLETED);
        map.put("begin",LocalDateTime.of(begin,LocalTime.MIN));
        map.put("end",LocalDateTime.of(end,LocalTime.MAX));
        List<Orders> ordersList = orderMapper.getOrdersByTime(map);

        //获得所有在此区间的订单详情


        if(ordersList==null|| ordersList.isEmpty()){
            return SalesTop10ReportVO.builder().build();
        }

        HashMap hashMap = new HashMap();
        for(Orders orders : ordersList){
            List<OrderDetail> orderDetailList= orderDetailMapper.list(orders);
            if(orderDetailList==null || orderDetailList.isEmpty()) continue;

            //查找相应详情
            for(OrderDetail orderDetail : orderDetailList){
                String name = orderDetail.getName();
                if(hashMap.containsKey(name)) hashMap.replace(name,(Integer)hashMap.get(name)+orderDetail.getNumber());
                else hashMap.put(name,orderDetail.getNumber());
            }
        }

        //前十排序
        //按value排序
        List<Map.Entry<String, Integer>> list = new ArrayList(hashMap.entrySet());
        Collections.sort(list,(o1,o2)->(o2.getValue().compareTo(o1.getValue())));


        //取出内容
        List<Map.Entry<String, Integer>> result = list.size()>=10? list.subList(0,9) :list;
        List names = new ArrayList(), times = new ArrayList<>();
        for(Map.Entry<String, Integer> salesMap : result){
            names.add(salesMap.getKey());
            times.add(salesMap.getValue());
        }
        return SalesTop10ReportVO
                .builder()
                .nameList(StringUtils.join(names,","))
                .numberList(StringUtils.join(times,","))
                .build();
    }

    private Integer getOrderCount(LocalDateTime begin,LocalDateTime end,Integer status){
        Map map =new HashMap();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status",status);
        return orderMapper.sumOrdersByMap(map);
    }
}
