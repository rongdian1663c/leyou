package com.leyou.order.interceptor;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.order.config.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    private JwtProperties prop;

    public LoginInterceptor(JwtProperties prop) {
        this.prop = prop;
    }

    private static final ThreadLocal<UserInfo> tl = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取cookie
        String token = CookieUtils.getCookieValue(request, prop.getCookieName());
        // 解析token
        try {
            // 获取用户信息
            UserInfo user = JwtUtils.getInfoFromToken(token, prop.getPublicKey());

            // 保存用户信息
            tl.set(user);
            return true;
        } catch (Exception e) {
            // 用户未登录或登录超时
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            log.info("【订单服务】用户未登录！");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 处理结束，移除user信息
        tl.remove();
    }

    public static UserInfo getUser(){
        return tl.get();
    }
}
