package com.leyou;


import com.leyou.item.pojo.Brand;
import com.leyou.item.pojo.Category;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.Spu;
import com.leyou.page.client.BrandClient;
import com.leyou.page.client.CategoryClient;
import com.leyou.page.client.GoodsClient;
import com.leyou.page.client.SpecificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class PageService {
    private static final ExecutorService es = Executors.newFixedThreadPool(20);

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specClient;

    @Value("${ly.page.destPath}")
    public String destPath;

    @Autowired
    private TemplateEngine templateEngine;

    public Map<String,Object> loadData(long spuId){
        // 查询spu
        Spu spu = goodsClient.querySpuById(spuId);
        // 查询分类
        List<Category> categories = categoryClient.queryCategoryByIds(
                Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        // 查询品牌
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        // 查询规格参数
        List<SpecGroup> specs = specClient.querySpecs(spu.getCid3());

        // 封装数据
        Map<String, Object> data = new HashMap<>();
        data.put("specs", specs);
        data.put("brand", brand);
        data.put("categories", categories);
        data.put("skus", spu.getSkus());
        data.put("detail", spu.getSpuDetail());

        // 防止重复数据
        spu.setSkus(null);
        spu.setSpuDetail(null);
        data.put("spu", spu);
        return data;
    }


    public void createHtml(Long spuId){
        createHtml(spuId,loadData(spuId));
    }

    private void createHtml(Long spuId, Map<String, Object> data)   {
        try {
            //准备上下文
            Context context = new Context();
            context.setVariables(data);

            //获取目标文件路径
            File dest = getDestFile(spuId);

            //判断目标文件是否存在
            if (dest.exists()){
                dest.delete();
            }

            //准备流
            try (PrintWriter writer = new PrintWriter(dest)){
                templateEngine.process("item",context,writer);
            }
        }catch (Exception e){
            log.error("页面创建失败, {}",e.getMessage(),e);
            throw new RuntimeException("创建页面失败");
        }
    }


    public void asyncCreateHtml(Long spuId){
        es.execute(() -> {
                createHtml(spuId);
        });
    }


    public void asyncCreateHtml(Long spuId , Map<String,Object>data){
        es.execute(() -> {
                createHtml(spuId, data);
        });
    }


    private File getDestFile(Long spuId) {
        //目标目录
        File dir = new File(destPath);
        if (!dir.exists())dir.mkdir();
        //返回文件地址
        return new File(dir,spuId+".html");
    }


    public void deleteHtml(Long spuId) {
        File file = getDestFile(spuId);
        if (file.exists()) {
            file.delete();
        }
    }
}
