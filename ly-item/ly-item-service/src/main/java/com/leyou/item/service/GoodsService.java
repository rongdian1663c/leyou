package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.CartDTO;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-19 11:48
 **/
@Service
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper detailMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;


    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, Boolean saleable, String key) {
        // 分页
        PageHelper.startPage(page, rows);
        // 过滤
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        // 过滤逻辑删除
        criteria.andEqualTo("valid", true);
        // 搜索条件
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%" + key + "%");
        }
        // 上下架
        if (saleable != null) {
            criteria.andEqualTo("saleable", saleable);
        }
        // 查询结果
        List<Spu> list = spuMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(HttpStatus.NO_CONTENT, "没有查询到商品信息");
        }
        // 查询分类名称和品牌名称
        handleCategoryAndBrandName(list);
        // 封装分页结果
        PageInfo<Spu> info = new PageInfo<>(list);
        return new PageResult<>(info.getTotal(), list);
    }

    private void handleCategoryAndBrandName(List<Spu> list) {
        for (Spu spu : list) {
            // 查询分类
            List<Category> categories = categoryService.queryCategoryByIds(
                    Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
//            List<String> ns = new ArrayList<>();
//            for (Category category : categories) {
//                ns.add(category.getName());
//            }
            if (categories == null) {
                throw new LyException(HttpStatus.NO_CONTENT, "分类不存在");
            }

            List<String> names = categories.stream()
                    .map(c -> c.getName()).collect(Collectors.toList());


            spu.setCname(StringUtils.join(names, "/"));
            // 查询品牌
            Brand brand = brandService.queryBrandById(spu.getBrandId());
            spu.setBname(brand.getName());
        }
    }

    @Transactional
    public void saveGoods(Spu spu) {
        // 新增spu
        spu.setId(null);
        spu.setSaleable(true);
        spu.setValid(true);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        spuMapper.insert(spu);

        // 新增detail
        spu.getSpuDetail().setSpuId(spu.getId());
        detailMapper.insert(spu.getSpuDetail());
        // 新增sku和库存
        saveSkuAndStock(spu);

        //发送消息
        amqpTemplate.convertAndSend("item.insert",spu.getId());

    }

    public SpuDetail queryDetailBySpuId(Long spuId) {
        return detailMapper.selectByPrimaryKey(spuId);
    }

    public List<Sku> querySkuListBySpuId(Long spuId) {
        // 查询sku
        Sku s = new Sku();
        s.setSpuId(spuId);
        List<Sku> list = skuMapper.select(s);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(HttpStatus.NO_CONTENT, null);
        }
        // 查询库存
        for (Sku sku : list) {
            Stock stock = stockMapper.selectByPrimaryKey(sku.getId());
            if(stock == null){
                throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "商品库存查询失败！");
            }
            sku.setStock(stock.getStock());
        }
        return list;
    }

    @Transactional
    public void updateGoods(Spu spu) {
        if(spu.getId() == null){
            throw new LyException(HttpStatus.BAD_REQUEST, "商品id不能为空");
        }
        // 先查询以前的sku
        Sku s = new Sku();
        s.setSpuId(spu.getId());
        List<Sku> skus = skuMapper.select(s);
        if(CollectionUtils.isNotEmpty(skus)){
            // 存在,则删除sku
            skuMapper.delete(s);
            // 删除库存
            List<Long> ids = skus.stream().map(sku -> sku.getId()).collect(Collectors.toList());
            stockMapper.deleteByIdList(ids);
        }
        // 修改spu
        spu.setLastUpdateTime(new Date());
        spu.setValid(null);
        spu.setSaleable(null);
        spu.setCreateTime(null);
        spuMapper.updateByPrimaryKeySelective(spu);
        // 修改detail
        detailMapper.updateByPrimaryKey(spu.getSpuDetail());

        // 新增sku和库存
        saveSkuAndStock(spu);

        //发送消息
        amqpTemplate.convertAndSend("item.update",spu.getId());
    }


    private void saveSkuAndStock(Spu spu) {
        // 新增sku
        List<Sku> skus = spu.getSkus();
        List<Stock> stocks = new ArrayList<>();

        for (Sku sku : skus) {
            // 对sku新增
            sku.setSpuId(spu.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            skuMapper.insert(sku);

            // 生成stock
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stocks.add(stock);
        }

        // 批量新增库存
        stockMapper.insertList(stocks);
    }

    public Spu querySpuById(Long id) {
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu == null){
            throw new LyException(HttpStatus.NOT_FOUND,"商品查询失败！");
        }
        //封装detail
        spu.setSpuDetail(queryDetailBySpuId(id));
        //封装skus
        spu.setSkus(querySkuListBySpuId(id));
        return spu;
    }

    public List<Sku> querySkuListByIds(List<Long> ids) {
        //查询sku
        List<Sku> skus = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(skus)) {
            throw new LyException(HttpStatus.NOT_FOUND, "sku查询失败");
        }
        loadStocks(ids, skus);
        return skus;
    }

    private void loadStocks(List<Long> ids, List<Sku> skus) {
        List<Stock> stocks = stockMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(stocks)) {
            throw new LyException(HttpStatus.NOT_FOUND, "sku的库存查询失败");
        }
        //把库存集合转为一个map,key是sku的id,值是库存值
        Map<Long,Integer> map = stocks.stream()
                .collect(Collectors.toMap(s -> s.getSkuId(),s -> s.getStock()));

        //填写库存
        for (Sku sku : skus) {
            sku.setStock(map.get(sku.getId()));
        }
    }

    @Transactional
    public void decreaseStock(List<CartDTO> cartDTOs) {
        for (CartDTO cartDTO : cartDTOs) {
            // 减库存
            int count = stockMapper.decreaseStock(cartDTO.getSkuId(), cartDTO.getNum());
            if(count != 1){
                throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "减库存失败");
            }
        }
    }
}
