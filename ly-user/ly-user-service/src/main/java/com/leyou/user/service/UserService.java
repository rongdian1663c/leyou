package com.leyou.user.service;

import com.leyou.User;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    private static final String KEY_PREFIX = "user:verify:phone:";
    public Boolean checkData(String data, Integer type) {
        //查询条件
        User user = new User();
        switch (type){
            case 1:
                user.setUsername(data);
                break;
            case 2:
                user.setPhone(data);
                break;
            default:
                throw new LyException(HttpStatus.BAD_REQUEST, "请求参数有误！");
        }
        //查询是否存在
        return userMapper.selectCount(user) == 0;
    }

    public void sendCode(String phone){
        //如果已存在,不发短信
        String key = KEY_PREFIX + phone;
        String code = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(code)) {
            return;
        }

        //验证手机号
        if (!phone.matches("^1[3456789]\\d{9}$")) {
            throw new LyException(HttpStatus.BAD_REQUEST, "手机号码不正确");
        }
        //生成随机验证码
        code = NumberUtils.generateCode(6);

        //保存验证码,5分钟
        redisTemplate.opsForValue().set(key,code,1, TimeUnit.MINUTES);

        //发送mq,通知sms发送短信
        Map<String,String> msg = new HashMap<>();
        msg.put("phone", phone);
        msg.put("code", code);
        amqpTemplate.convertAndSend("ly.sms.exchange", "sms.verify.code", msg);
    }

    public void register(User user,String code){
        //补充字段
        user.setId(null);
        user.setCreated(new Date());
        //取出redis中的验证码
        String key = KEY_PREFIX + user.getPhone();
        String cacheCode = redisTemplate.opsForValue().get(key);
        //校验验证码
        if (!StringUtils.equals(code,cacheCode)) {
            throw new LyException(HttpStatus.BAD_REQUEST, "验证码不正确");
        }
        //生成盐
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);

        //对密码加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(),salt));

        //写入数据库
        int count = userMapper.insert(user);
        if (count != 1) {
            throw new LyException(HttpStatus.BAD_REQUEST, "参数有误");
        }

        redisTemplate.delete(key);
    }

    public User queryUserByUsernameAndPassword(String username, String password) {
        //根据用户名查询
        User user = new User();
        user.setUsername(username);
        user = userMapper.selectOne(user);
        //判断用户名
        if (user == null){
            //用户名不存在,返回400
            throw new LyException(HttpStatus.BAD_REQUEST, "用户名或密码不正确");
        }
        //校验密码
        if (!user.getPassword().equals(CodecUtils.md5Hex(password, user.getSalt()))) {
            // 密码不正确
            throw new LyException(HttpStatus.BAD_REQUEST, "用户名或密码不正确");
        }
        //用户和密码都正确
        return user;
    }
}