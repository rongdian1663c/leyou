package com.leyou.auth.controller;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.service.AuthService;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.CookieUtils;
import jdk.internal.org.objectweb.asm.tree.TryCatchBlockNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@EnableConfigurationProperties({JwtProperties.class})
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProperties prop;

    /***
     * 登录功能
     */
    @PostMapping("login")
    public ResponseEntity<Void> login(
       @RequestParam("username") String username,@RequestParam("password") String password ,HttpServletRequest request,HttpServletResponse response
    ){
        //登录
        String token = authService.login(username,password);
        if (StringUtils.isBlank(token)) {
            throw new LyException(HttpStatus.BAD_REQUEST, "账号或密码错误");
        }
        //写入cookie
        CookieUtils.newBuilder(response).httpOnly().request(request).build(prop.getCookieName(),token);
        return ResponseEntity.ok().build();
    }


    /**
    * 校验用户登录状态
     * *
    */
    @GetMapping("verify")
    public ResponseEntity<UserInfo> verifyUser(
            @CookieValue("LY_TOKEN") String token,
            HttpServletRequest request,HttpServletResponse response){
        try {
            //解析token,获取用户信息
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, prop.getPublicKey());
            if (userInfo == null){
                throw new LyException(HttpStatus.BAD_REQUEST, "请求凭证不完整");
            }
            //刷新token
            token = JwtUtils.generateTokenInMinutes(userInfo,prop.getPrivateKey(),prop.getExpire());
            //写回cookie
            CookieUtils.newBuilder(response)
                    .httpOnly().request(request).build(prop.getCookieName(),token);

            //返回用户
            return ResponseEntity.ok(userInfo);
        }catch (Exception e){
            throw new LyException(HttpStatus.UNAUTHORIZED, "未登录或登录超时");
        }
    }

}
