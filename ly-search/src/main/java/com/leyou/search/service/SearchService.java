package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.utils.NumberUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;



@Service
@Slf4j
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specClient;

    @Autowired
    private GoodsRepository repository;

    @Autowired
    private ElasticsearchTemplate template;

    public Goods buildGoods(Spu spu) {
        Long spuId = spu.getId();
        // 查询分类
        List<String> names = categoryClient.queryCategoryByIds(
                Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3())).stream()
                .map(c -> c.getName()).collect(Collectors.toList());
        // 查询品牌
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        // 拼接查询条件
        String all = spu.getTitle() + " " + StringUtils.join(names, " ") + " " + brand.getName();

        // 查询sku
        List<Sku> skus = goodsClient.querySkuListBySpuId(spuId);
        // 价格
        Set<Long> prices = new HashSet<>();
        // 处理sku
        List<Map<String, Object>> skuList = new ArrayList<>();
        for (Sku sku : skus) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", sku.getId());
            map.put("title", sku.getTitle());
            map.put("image", StringUtils.substringBefore(sku.getImages(), ","));
            map.put("price", sku.getPrice());
            skuList.add(map);
            // 保存价格
            prices.add(sku.getPrice());
        }

        // 查询当前分类下可以用来过滤的规格参数
        List<SpecParam> params = specClient.queryParams(null, spu.getCid3(), null, true);
        // 查询spuDetail
        SpuDetail detail = goodsClient.queryDetailBySpuId(spuId);
        // 取出通用规格参数
        Map<String, Object> genericSpec = JsonUtils.nativeRead(detail.getGenericSpec(),
                new TypeReference<Map<String, Object>>() {
                });
        // 取出特有规格参数
        Map<String, List<Object>> specialSpec = JsonUtils.nativeRead(detail.getSpecialSpec(),
                new TypeReference<Map<String, List<Object>>>() {
                });
        // 规格参数，其key是规格参数名称（tb_spec_param），其值在spuDetail中
        Map<String, Object> specs = new HashMap<>();
        // 组装规格参数
        for (SpecParam param : params) {
            String key = param.getName();
            Object value = null;
            // 判断是否是通用属性
            if (param.getGeneric()) {
                // 通用属性
                value = genericSpec.get(param.getId().toString());
                // 判断是否是数值类型
                if (param.getNumeric()) {
                    // 如果是数值类型，还需要分段
                    value = chooseSegment(value.toString(), param);
                }
            } else {
                // 不通用
                value = specialSpec.get(param.getId().toString());
            }
            if (value == null) {
                value = "其它";
            }
            specs.put(key, value);
        }

        Goods goods = new Goods();
        goods.setAll(all);
        goods.setSpecs(specs);
        goods.setPrice(prices);
        goods.setSkus(JsonUtils.toString(skuList));
        goods.setSubTitle(spu.getSubTitle());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setId(spuId);
        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public PageResult<Goods> search(SearchRequest request) {
        String key = request.getKey();
        if (StringUtils.isBlank(key)) {
            // 没有查询条件
            throw new LyException(HttpStatus.BAD_REQUEST, "查询条件不能为空");
        }
        // 原生查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 0、控制返回结果字段
        queryBuilder.withSourceFilter(
                new FetchSourceFilter(new String[]{"id", "subTitle", "skus"}, new String[]{}));
        // 1、分页
        int page = request.getPage() - 1;
        int size = request.getSize();
        queryBuilder.withPageable(PageRequest.of(page, size));

        // 2、基本搜索条件

        QueryBuilder basicQuery = buildBasicQuery(request);
        queryBuilder.withQuery(basicQuery);

        // 3、对分类和品牌聚合
        String categoryAggName = "categoryAgg";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        String brandAggName = "brandAgg";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        // 4、搜索
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);

        // 5、解析结果
        // 5.1、解析聚合结果
        Aggregations aggs = result.getAggregations();
        List<Category> categories = handleCategoryAgg(aggs.get(categoryAggName));
        List<Brand> brands = handleBrandAgg(aggs.get(brandAggName));

        // 5.2、对规格参数聚合
        List<Map<String, Object>> specs = null;
        // 判断分类数量是否为1
        if (categories != null && categories.size() == 1) {
            specs = handleSpecs(categories.get(0).getId(), basicQuery);
        }

        // 5.3、解析分页结果
        long total = result.getTotalElements();
        int totalPages = result.getTotalPages();
        List<Goods> content = result.getContent();

        return new SearchResult(total, totalPages, content, categories, brands, specs);
    }

    private QueryBuilder buildBasicQuery(SearchRequest request) {
        // 构建布尔查询
        BoolQueryBuilder basicQuery = QueryBuilders.boolQuery();
        // 搜索条件
        basicQuery.must(QueryBuilders.matchQuery("all", request.getKey()));
        // 过滤条件
        Map<String, String> filterMap = request.getFilter();
        for (Map.Entry<String, String> entry : filterMap.entrySet()) {
            // 过滤字段
            String key = entry.getKey();
            if (!"cid3".equals(key) && !"brandId".equals(key)) {
                key = "specs." + key + ".keyword";
            }
            // 过滤条件
            String value = entry.getValue();
            // 因为是keyword类型，所以使用term查询
            basicQuery.filter(QueryBuilders.termQuery(key, value));
        }
        return basicQuery;
    }

    private List<Map<String, Object>> handleSpecs(Long cid, QueryBuilder basicQuery) {
        List<Map<String, Object>> specs = new ArrayList<>();
        // 1、查询可过滤的规格参数
        List<SpecParam> params = specClient.queryParams(null, cid, null, true);
        // 2、聚合规格参数
        // 2.1 基本查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(basicQuery);
        queryBuilder.withPageable(PageRequest.of(0, 1));
        // 2.2 聚合
        for (SpecParam param : params) {
            String name = param.getName();
            queryBuilder.addAggregation(AggregationBuilders.terms(name).field("specs." + name + ".keyword"));
        }
        // 3、查询
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);

        // 4、解析结果
        Aggregations aggs = result.getAggregations();
        for (SpecParam param : params) {
            String name = param.getName();
            StringTerms terms = aggs.get(name);
            // 创建聚合结果
            Map<String, Object> map = new HashMap<>();
            map.put("k", name);
            map.put("options", terms.getBuckets().stream().map(b -> b.getKeyAsString()).collect(Collectors.toList()));
            specs.add(map);
        }
        // 4、封装
        return specs;
    }

    private List<Brand> handleBrandAgg(LongTerms terms) {
        try {
            // 获取id
            List<Long> ids = terms.getBuckets().stream()
                    .map(b -> b.getKeyAsNumber().longValue()).collect(Collectors.toList());
            // 查询
            List<Brand> brands = brandClient.queryBrandByIds(ids);
            return brands;
        } catch (Exception e) {
            log.error("查询品牌信息失败", e);
            return null;
        }
    }

    private List<Category> handleCategoryAgg(LongTerms terms) {
        try {
            // 获取id
            List<Long> ids = terms.getBuckets().stream()
                    .map(b -> b.getKeyAsNumber().longValue()).collect(Collectors.toList());
            // 查询
            List<Category> categories = categoryClient.queryCategoryByIds(ids);
            for (Category c : categories) {
                c.setParentId(null);
                c.setIsParent(null);
                c.setSort(null);
            }
            return categories;
        } catch (Exception e) {
            log.error("查询分类信息失败", e);
            return null;
        }
    }
}
