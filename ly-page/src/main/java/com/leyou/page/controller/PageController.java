package com.leyou.page.controller;


import com.leyou.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.FileNotFoundException;
import java.util.Map;

@Controller
@RequestMapping
public class PageController {

    @Autowired
    private PageService pageService;

    @GetMapping("item/{id}.html")
    public String hello(Model model, @PathVariable("id") Long spuId) {
        // 加载数据
        Map<String, Object> data = pageService.loadData(spuId);
        // 存放模型数据
        model.addAllAttributes(data);

        // 生成静态页面
        pageService.asyncCreateHtml(spuId, data);
        return "item";
    }
}
