package com.leyou.cart.service;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.cart.interceptor.LoginInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "ly:cart:uid";

    public void addCart(Cart cart) {
        Integer num = cart.getNum();

        //获取登录用户
        UserInfo user = LoginInterceptor.getUser();

        //获取已登录购物车
        String key = KEY_PREFIX + user.getId();
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(key);

        //判断当前商品是否存在
        String hashKey = cart.getSkuId().toString();
        if (hashOps.hasKey(hashKey)) {
            //存在,则修改数量
            String json = hashOps.get(hashKey);
            cart = JsonUtils.toBean(json,Cart.class);
            cart.setNum(cart.getNum() + num);
        }

        //写回redis
        hashOps.put(hashKey,JsonUtils.toString(cart));
    }



    public List<Cart> queryCartlist() {
        //获取登录用户
        UserInfo user = LoginInterceptor.getUser();

        //获取已登录的购物车
        String key = KEY_PREFIX + user.getId();

        //判断是否存在
        if (!redisTemplate.hasKey(key)) {
            throw new LyException(HttpStatus.NOT_FOUND,"购物车为空");
        }

        //获取购物车
        return redisTemplate.boundHashOps(key).values().stream()
                .map(o -> JsonUtils.toBean(o.toString(),Cart.class)).collect(Collectors.toList());
    }


    public void updateNum(Long id, Integer num) {
        //获取登录用户
        UserInfo user = LoginInterceptor.getUser();
        //获取已登录购物车
        String key = KEY_PREFIX + user.getId();
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(key);
        //判断当前商品是否存在
        String hashKey = id.toString();

        if (!hashOps.hasKey(hashKey)){
            throw new LyException(HttpStatus.BAD_REQUEST, "购物车数据不存在！");
        }

        //查询购物车
        String json = hashOps.get(hashKey);
        Cart cart = JsonUtils.toBean(json, Cart.class);
        cart.setNum(num);
    }



    public void deleteCartById(Long id) {
        // 获取登录用户
        UserInfo user = LoginInterceptor.getUser();
        // 获取已登录购物车
        String key = KEY_PREFIX + user.getId();
        // 删除
        redisTemplate.opsForHash().delete(key, id.toString());
    }
}
