package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.properties.ShopLocationProperties;
import com.sky.result.LocationQueryResult;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.HttpClientUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GeodeticCurve;
import org.gavaghan.geodesy.GlobalCoordinates;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sky.utils.WeChatPayUtil;
import org.springframework.web.util.UriUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DishMapper dishMapper;


    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private WebSocketServer webSocketServer;


    @Autowired
    private ShopLocationProperties shopLocationProperties;
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //异常情况的处理（收货地址为空、超出配送范围、购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        try {
            boolean addressValid = addressValidation(addressBook);
            if(addressValid == false) throw new AddressBookBusinessException(MessageConstant.USER_IS_TOO_LONG);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //异常情况的处理（购物车为空）
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //构造订单数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(userId);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setOrderTime(LocalDateTime.now());
        //向订单表插入1条数据
        orderMapper.insert(order);

        //订单明细数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }

        //向明细表插入n条数据
        orderDetailMapper.insertBatch(orderDetailList);

        //清理购物车中的数据
        ShoppingCart shoppingCart1 = new ShoppingCart();
        shoppingCart1.setUserId(userId);
        shoppingCartMapper.clean(shoppingCart1);

        //封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();

        return orderSubmitVO;

    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //websocket 推送消息
        HashMap map = new HashMap();
        map.put("type",1);
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号："+ outTradeNo);

        String json = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(json);

    }
    @Override
    @Transactional
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO, Integer userType){
        //查出所有订单
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        //用户调用才设置
        if(userType == 1) ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        Long total = page.getTotal();
        //查找所有订单详情
        List<OrderVO> result = new ArrayList<OrderVO>();
        for(Orders orders: page){
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders,orderVO);

            List<OrderDetail> list= orderDetailMapper.list(orders);
            orderVO.setOrderDetailList(list);
            result.add(orderVO);
        }

        return new PageResult(total,result);
    }

    @Override
    @Transactional
    public void repetition(Long id){
        //根据订单id查询
        Orders orders = orderMapper.getByOrderId(id);
        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        List<OrderDetail> orderDetailList = orderDetailMapper.list(orders);

        shoppingCartMapper.clean(ShoppingCart.builder().userId(BaseContext.getCurrentId()).build());

        for(OrderDetail orderDetail : orderDetailList) {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);

            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
            continue;
        }
    }

    @Override
    public OrderVO orderDeatil(Long id){
        Orders orders = orderMapper.getByOrderId(id);
        orders.setUserId(BaseContext.getCurrentId());
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);

        List<OrderDetail> list = orderDetailMapper.list(orders);
        orderVO.setOrderDetailList(list);
        return orderVO;
    }

    @Override
    public void  cancel(OrdersCancelDTO ordersCancelDTO){
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersCancelDTO,orders);

        orderMapper.cancel(ordersCancelDTO);
    }
    @Override
    public OrderStatisticsVO statistics(){
        Integer toBeConfirmed= orderMapper.statistics(2);
        Integer confirmed= orderMapper.statistics(3);
        Integer deliveryInProgress= orderMapper.statistics(4);
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(Long id){
        Orders orders = Orders.builder().id(id).status(Orders.CONFIRMED).build();
        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id){
        Orders orders = Orders.builder().id(id).status(Orders.DELIVERY_IN_PROGRESS).build();
        orderMapper.update(orders);
    }
    @Override
    public void complete(Long id){
        Orders orders = Orders.builder().id(id).status(Orders.COMPLETED).build();
        orderMapper.update(orders);
    }
    @Override
    public void reminder(Long id){
        Orders orders = orderMapper.getByOrderId(id);

        if(orders==null) throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);

        HashMap hashMap = new HashMap();
        hashMap.put("type",2);
        hashMap.put("OrderId",id);
        hashMap.put("content","订单号："+orders.getNumber());

        webSocketServer.sendToAllClient(JSON.toJSONString(hashMap));
    }


    //百度地图位置服务
    private static String URL = "https://api.map.baidu.com/geocoding/v3?";
    @Override
    public boolean addressValidation(AddressBook addressBook) throws Exception{
        String URL = "https://api.map.baidu.com/geocoding/v3?";
        String userAddress = addressBook.getProvinceName()+addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail();

        Map params = new LinkedHashMap<String, String>();
        //查询用户经纬度
        params.put("address", userAddress);
        params.put("output", "json");
        params.put("ak", shopLocationProperties.getBaiduak());
        params.put("sn", caculateSn(params));

        String userLocationQuery = requestLocation(URL,params);

        JSONObject jsonObject = JSON.parseObject(userLocationQuery);
        Location userlocation = jsonObject.getObject("result", LocationQuery.class).getLocation();
        Integer userQueryStatus =jsonObject.getInteger("status");

                Map userParam = new LinkedHashMap();
        //商户经纬度计算
        userParam.put("address", shopLocationProperties.getAddress());
        userParam.put("output", "json");
        userParam.put("ak", shopLocationProperties.getBaiduak());
        userParam.put("sn", caculateSn(userParam));

        String adminLocationQuery = requestLocation(URL,userParam);

        jsonObject = JSON.parseObject(adminLocationQuery);
        Location adminLocation = jsonObject.getObject("result", LocationQuery.class).getLocation();
        Integer adminQueryStatus =jsonObject.getInteger("status");

        if(userQueryStatus!=0 || adminQueryStatus!=0){
            throw new AddressBookBusinessException(MessageConstant.ERROR_FROM_BAIDUMAP);
        }
        GeodeticCurve geodeticCurve = new GeodeticCalculator().calculateGeodeticCurve(Ellipsoid.Sphere,new GlobalCoordinates(userlocation.getLat(),userlocation.getLng()),new GlobalCoordinates(adminLocation.getLat(),adminLocation.getLng()));
        Double distance = geodeticCurve.getEllipsoidalDistance();
        //大于五公里不配送
        return distance>5000.0 ? false : true;
    }

    private String requestLocation(String strUrl, Map<String, String> param) throws Exception {
        if (strUrl == null || strUrl.length() <= 0 || param == null || param.size() <= 0) {
            return null;
        }
        StringBuffer queryString = new StringBuffer();
        for (Map.Entry<?, ?> pair : param.entrySet()) {
            queryString.append(pair.getKey() + "=");
            //    第一种方式使用的 jdk 自带的转码方式  第二种方式使用的 spring 的转码方法 两种均可
            //    queryString.append(URLEncoder.encode((String) pair.getValue(), "UTF-8").replace("+", "%20") + "&");
            queryString.append(UriUtils.encode((String) pair.getValue(), "UTF-8") + "&");
        }
        if (queryString.length() > 0) {
            queryString.deleteCharAt(queryString.length() - 1);
        }

        return HttpClientUtil.doGet(URL,param);
    }
    private String caculateSn(Map paramsMap) throws UnsupportedEncodingException,
            NoSuchAlgorithmException {

        // 计算sn跟参数对出现顺序有关，get请求请使用LinkedHashMap保存<key,value>，该方法根据key的插入顺序排序；post请使用TreeMap保存<key,value>，该方法会自动将key按照字母a-z顺序排序。
        // 所以get请求可自定义参数顺序（sn参数必须在最后）发送请求，但是post请求必须按照字母a-z顺序填充body（sn参数必须在最后）。
        // 以get请求为例：http://api.map.baidu.com/geocoder/v2/?address=百度大厦&output=json&ak=yourak，paramsMap中先放入address，再放output，然后放ak，放入顺序必须跟get请求中对应参数的出现顺序保持一致。


        // 调用下面的toQueryString方法，对LinkedHashMap内所有value作utf8编码，拼接返回结果address=%E7%99%BE%E5%BA%A6%E5%A4%A7%E5%8E%A6&output=json&ak=yourak
        String paramsStr = toQueryString(paramsMap);

        // 对paramsStr前面拼接上/geocoder/v2/?，后面直接拼接yoursk得到/geocoder/v2/?address=%E7%99%BE%E5%BA%A6%E5%A4%A7%E5%8E%A6&output=json&ak=yourakyoursk
        String wholeStr = new String("/geocoding/v3?" + paramsStr + shopLocationProperties.getBaidusk());

        System.out.println(wholeStr);
        // 对上面wholeStr再作utf8编码
        String tempStr = URLEncoder.encode(wholeStr, "UTF-8");

        // 调用下面的MD5方法得到最后的sn签名
        String sn = MD5(tempStr);
        System.out.println(sn);
        return sn;
    }

    private String toQueryString(Map<?, ?> data)
            throws UnsupportedEncodingException {
        StringBuffer queryString = new StringBuffer();
        for (Map.Entry<?, ?> pair : data.entrySet()) {
            queryString.append(pair.getKey() + "=");
            //    第一种方式使用的 jdk 自带的转码方式  第二种方式使用的 spring 的转码方法 两种均可
            //    queryString.append(URLEncoder.encode((String) pair.getValue(), "UTF-8").replace("+", "%20") + "&");
            queryString.append(UriUtils.encode((String) pair.getValue(), "UTF-8") + "&");
        }
        if (queryString.length() > 0) {
            queryString.deleteCharAt(queryString.length() - 1);
        }
        return queryString.toString();
    }

    private String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest
                    .getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100)
                        .substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }
}
