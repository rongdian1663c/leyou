package com.leyou.item.api;

import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.CartDTO;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GoodsApi {
    /**
     * 根据spu查询detail
     *
     */
    @GetMapping("/spu/detail/{spuId}")
    SpuDetail queryDetailBySpuId(@PathVariable("spuId")Long spuId);

    /*
     * 根据spnu查询sku集合
     */
    @GetMapping("sku/list")
    List<Sku> querySkuListBySpuId(@RequestParam("id") Long spuId);

    /**
     *分页查询spu
     */
    @GetMapping("spu/page")
    PageResult<Spu> querySpuByPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "5") Integer rows,
            @RequestParam(value = "saleable", required = false) Boolean saleable,
            @RequestParam(value = "key", required = false) String key
    );



    /**
     * 根据spu的id查询spu
     */
    @GetMapping("spu/{id}")
    Spu querySpuById(@PathVariable("id") Long id);


    /**
     * 根据sku的ID集合查询sku
     */
    @GetMapping("/sku/list/ids")
    List<Sku> querySkuListByIds(@RequestParam("ids")List<Long> ids);


    /**
     * 减库存接口
     */
    @PostMapping("stock/decrease")
    void decreaseStock(@RequestBody List<CartDTO> cartDTOS);
}