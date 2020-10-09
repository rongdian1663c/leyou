package com.leyou.common.mapper;

import tk.mybatis.mapper.additional.idlist.IdListMapper;
import tk.mybatis.mapper.additional.insert.InsertListMapper;
import tk.mybatis.mapper.annotation.RegisterMapper;
import tk.mybatis.mapper.common.Mapper;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-20 10:43
 **/
@RegisterMapper
public interface BaseMapper<T,PK> extends Mapper<T>, IdListMapper<T, PK>, InsertListMapper<T> {
}
