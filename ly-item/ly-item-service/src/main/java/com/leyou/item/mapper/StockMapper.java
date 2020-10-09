package com.leyou.item.mapper;

import com.leyou.common.mapper.BaseMapper;
import com.leyou.item.pojo.Stock;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-20 10:47
 **/
public interface StockMapper extends BaseMapper<Stock, Long> {

    @Update("UPDATE tb_stock SET stock = stock - #{num} WHERE sku_id = #{id} AND stock > #{num}")
    int decreaseStock(@Param("id") Long id, @Param("num") Integer num);
}
