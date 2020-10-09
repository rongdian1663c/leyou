package com.leyou.item.service;

import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.pojo.Category;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-16 10:23
 **/
@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    public List<Category> queryCategoryListByPid(Long pid) {
        Category c = new Category();
        c.setParentId(pid);
        List<Category> list = categoryMapper.select(c);
        if (CollectionUtils.isEmpty(list)) {
            // 没找到，返回404
            throw new LyException(HttpStatus.NOT_FOUND, "该分类下没有子分类");
        }
        return list;
    }

    public List<Category> queryCategoryByIds(List<Long> ids){
        List<Category> list = categoryMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(list)) {
            // 没找到，返回404
            throw new LyException(HttpStatus.NOT_FOUND, "该分类不存在");
        }
        return list;
    }
}
