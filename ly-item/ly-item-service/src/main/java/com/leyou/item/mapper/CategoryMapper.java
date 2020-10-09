package com.leyou.item.mapper;

import com.leyou.item.pojo.Category;
import tk.mybatis.mapper.additional.idlist.IdListMapper;
import tk.mybatis.mapper.common.Mapper;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-16 10:22
 **/
public interface CategoryMapper extends Mapper<Category>, IdListMapper<Category, Long> {
}
