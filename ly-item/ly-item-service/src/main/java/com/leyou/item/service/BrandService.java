package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-16 12:13
 **/
@Service
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;

    public PageResult<Brand> queryBrandByPage(Integer page, Integer rows, String sortBy, Boolean desc, String key) {
        // 分页
        PageHelper.startPage(page, rows);
        // 过滤
        Example example = new Example(Brand.class);
        if (StringUtils.isNotBlank(key)) {
            example.createCriteria().orLike("name", "%" + key + "%")
                    .orEqualTo("letter", key.toUpperCase());
        }
        // 排序
        if (StringUtils.isNotBlank(sortBy)) {
            example.setOrderByClause(sortBy + (desc ? " DESC" : " ASC"));
        }
        // 查询结果
        List<Brand> list = brandMapper.selectByExample(example);

        if(CollectionUtils.isEmpty(list)){
            throw new LyException(HttpStatus.NOT_FOUND, "品牌查询失败");
        }

        // 封装分页对象
        PageInfo<Brand> info = new PageInfo<>(list);

        // 返回
        return new PageResult<>(info.getTotal(), list);
    }

    @Transactional
    public void saveBrand(Brand brand, List<Long> ids) {
        brand.setId(null);
        int count = brandMapper.insert(brand);
        if(count == 0){
            throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "新增品牌失败！");
        }
        for (Long cid : ids) {
            count = brandMapper.saveCategoryBrand(cid, brand.getId());
            if(count == 0){
                throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "新增品牌和分类失败！");
            }
        }
    }

    public Brand queryBrandById(Long id){
        Brand brand = brandMapper.selectByPrimaryKey(id);
        if(brand == null){
            throw new LyException(HttpStatus.NO_CONTENT, null);
        }
        return brand;
    }

    public List<Brand> queryBrandByCid(Long cid) {
        List<Brand> list = brandMapper.queryByCategoryId(cid);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(HttpStatus.NO_CONTENT, null);
        }
        return list;
    }

    public List<Brand> queryByIds(List<Long> ids) {
        List<Brand> list = brandMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(HttpStatus.NOT_FOUND, "没有查询到品牌");
        }
        return list;
    }
}
