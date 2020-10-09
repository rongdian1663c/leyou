package com.leyou.auth.service;

import com.leyou.User;
import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {

    @Autowired
    private JwtProperties prop;

    @Autowired
    private UserClient userClient;

    public String login(String username, String password){

        try {
            // 查询用户
            User user = userClient.queryUserByUsernameAndPassword(username, password);
            // 判断是否为空
            if (user == null) {
                return null;
            }
            // 生成userInfo
            UserInfo userInfo = new UserInfo(user.getId(), user.getUsername());
            // 生成token
            String token = JwtUtils.generateTokenInMinutes(userInfo, prop.getPrivateKey(), prop.getExpire());
            // 返回
            return token;
        } catch (Exception e) {
            return null;
        }
    }




}
