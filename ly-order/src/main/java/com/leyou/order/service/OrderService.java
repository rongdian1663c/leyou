package com.leyou.order.service;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.IdWorker;
import com.leyou.item.pojo.Sku;
import com.leyou.order.client.GoodsClient;
import com.leyou.order.constants.AddressConstants;
import com.leyou.order.dto.AddressDTO;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.enums.PayStatusEnum;
import com.leyou.order.interceptor.LoginInterceptor;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.mapper.PayLogMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.order.pojo.PayLog;
import com.leyou.order.utils.PayHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper detailMapper;

    @Autowired
    private OrderStatusMapper statusMapper;

    @Autowired
    private PayHelper payHelper;

    @Autowired
    private PayLogService payLogService;

    @Autowired
    private PayLogMapper logMapper;

    @Transactional
    public Long createOrder(OrderDTO orderDTO) {
        // 0.生成订单的id
        long orderId = idWorker.nextId();

        // 1、组织order数据
        Order order = new Order();

        // 1.1.订单基本数据
        order.setOrderId(orderId);
        order.setPaymentType(orderDTO.getPaymentType());
        order.setPostFee(0L);//todo 无法调用物流,根据地址计算邮费
        order.setCreateTime(new Date());

        // 1.2.用户数据
        //获取用户信息
        UserInfo user = LoginInterceptor.getUser();
        order.setUserId(user.getId());
        order.setBuyerNick(user.getUsername());
        order.setBuyerRate(false);

        // 1.3.收货人信息
        @NotNull Long addressId = orderDTO.getAddressId();
        AddressDTO address = AddressConstants.findById(addressId);
        if (address == null) {
            // 商品不存在，抛出异常
            throw new LyException(HttpStatus.BAD_REQUEST, "收获地址不存在");
        }
        order.setReceiver(address.getName());
        order.setReceiverState(address.getState());
        order.setReceiverCity(address.getCity());
        order.setReceiverDistrict(address.getDistrict());
        order.setReceiverAddress(address.getAddress());
        order.setReceiverZip(address.getZipCode());
        order.setReceiverMobile(address.getPhone());

        // 1.4.付款金额相关
        Map<Long,Integer> skuNumMap =  orderDTO.getCarts()
                .stream().collect(Collectors.toMap(c -> c.getSkuId(),c -> c.getNum()));
        //查询商品
        List<Sku> skus = goodsClient.querySkuListByIds(new ArrayList<>(skuNumMap.keySet()));
        if (CollectionUtils.isEmpty(skus)) {
            // 商品不存在，抛出异常
            throw new LyException(HttpStatus.BAD_REQUEST, "商品不存在, 无法下单");
        }
        Long totalPay = 0L;

        // 2、组织OrderDetail数据
        ArrayList<OrderDetail> orderDetails = new ArrayList<>();
        for (Sku sku : skus) {
            Integer num = skuNumMap.get(sku.getId());
            totalPay += sku.getPrice() * num;
            //组织detail
            OrderDetail detail = new OrderDetail();
            detail.setOrderId(orderId);
            detail.setOwnSpec(sku.getOwnSpec());
            detail.setTitle(sku.getTitle());
            detail.setPrice(sku.getPrice());
            detail.setSkuId(sku.getId());
            detail.setNum(num);
            detail.setImage(StringUtils.substringBefore(sku.getImages(), ","));
            orderDetails.add(detail);
        }
        order.setTotalPay(totalPay);
        order.setActualPay(totalPay + order.getPostFee());//todo 还要减去优惠金额

        //保存order
        orderMapper.insertSelective(order);

        //保存detail
        detailMapper.insertList(orderDetails);

        // 3、组织OrderStatus数据
        OrderStatus status = new OrderStatus();
        status.setOrderId(orderId);
        status.setCreateTime(new Date());
        status.setStatus(OrderStatusEnum.INIT.value());

        statusMapper.insertSelective(status);

        // 4、减库存
        goodsClient.decreaseStock(orderDTO.getCarts());

        log.info("生成订单，订单编号：{}，用户id：{}", orderId, user.getId());
        return orderId;

    }


    public String createPayUrl(Long orderId) {
        //查询订单
        Order order = orderMapper.selectByPrimaryKey(orderId);
        //订单金额
        Long actualPay = 1L;
        //描述
        String description = "乐优商城测试";

        //校验订单状态
        OrderStatus status = statusMapper.selectByPrimaryKey(orderId);
        if (status.getStatus() != OrderStatusEnum.INIT.value()) {
            // 订单已付款或已结束
            throw new LyException(HttpStatus.BAD_REQUEST, "订单已支付或结束！");
        }

        //校验登录用户
        UserInfo user = LoginInterceptor.getUser();
        if (user.getId() != order.getUserId()) {
            // 用户不正确
            throw new LyException(HttpStatus.BAD_REQUEST, "下单用户不正确！");
        }

        String payUrl = payHelper.createPayUrl(orderId, actualPay, description);

        //记录支付日志
        payLogService.createPayLog(orderId, order.getActualPay());
        return payUrl;
    }



    public Order  queryOrderById(Long orderId) {
        //查询订单
        Order order = orderMapper.selectByPrimaryKey(orderId);
        //查询订单详情
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(orderId);
        List<OrderDetail> details = detailMapper.select(detail);
        order.setOrderDetails(details);
        //订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        order.setOrderStatus(orderStatus);
        return order;

    }


    @Transactional
    public void payNotify(Map<String, String> request) {
        payHelper.handleNotify(request);
    }


    public Integer  queryOrderState(Long orderId) {
        //先查询本地
        PayLog log = logMapper.selectByPrimaryKey(orderId);
        if (log == null || log.getStatus() == PayStatusEnum.NOT_PAY.value()){
            //如果日志不存在,或者未支付,name求微信查询
            PayState payState = payHelper.queryPayState(orderId);
            return payState.getValue();
        }
        if(log.getStatus() == PayStatusEnum.SUCCESS.value()){
            // 支付成功，返回1
            return PayState.SUCCESS.getValue();
        }
        // 失败，返回2
        return PayState.FAIL.getValue();
    }

}
