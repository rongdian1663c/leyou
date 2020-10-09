package com.leyou.page.pojo;

import lombok.Data;

@Data
public class User {
    String name;
    int age;
    User friend;// 对象类型属性

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public User() {
    }
}