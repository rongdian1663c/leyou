package com.leyou.item.api;

import com.leyou.item.pojo.Brand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-23 10:09
 **/

@RequestMapping("brand")
public interface BrandApi {
    @GetMapping("{id}")
    Brand queryBrandById(@PathVariable("id") Long id);

    @GetMapping("list")
    List<Brand> queryBrandByIds(@RequestParam("ids")List<Long> ids);
}
