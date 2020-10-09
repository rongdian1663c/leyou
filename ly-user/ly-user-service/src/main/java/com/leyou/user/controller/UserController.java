package com.leyou.user.controller;

import com.leyou.User;
import com.leyou.common.exception.LyException;
import com.leyou.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.apache.commons.lang3.StringUtils;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 校验数据是否可用
     */
    @GetMapping("/check/{data}/{type}")
    public ResponseEntity<Boolean> checkData(
            @PathVariable("data")String data, @PathVariable(value = "type",required = false) Integer type
    ){
        //初始化type类型
        if (type == null)type = 1;
        //校验数据
        return ResponseEntity.ok(userService.checkData(data,type));
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/code")
    public ResponseEntity<Void> sendCode(@RequestParam("phone")String phone){
        userService.sendCode(phone);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 注册用户
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid User user, BindingResult result,@RequestParam("code")String code){
        //todo 完成数据格式校验
        if (result.hasFieldErrors()) {
            List<String> msgs = result.getFieldErrors().stream()
                    .map(e -> e.getDefaultMessage()).collect(Collectors.toList());
            throw new LyException(HttpStatus.BAD_REQUEST, StringUtils.join(msgs, "|"));
        }
        userService.register(user,code);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 根据用户名和密码查询用户
     */
    @GetMapping("/query")
    public ResponseEntity<User> queryUserByUsernameAndPassword(
            @RequestParam("username")String username,@RequestParam("password")String password){
        return ResponseEntity.ok(userService.queryUserByUsernameAndPassword(username, password));
    }
}
