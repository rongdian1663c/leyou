package com.leyou.item.service;

import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-19 09:54
 **/
@Service
public class SpecificationService {

    @Autowired
    private SpecGroupMapper groupMapper;

    @Autowired
    private SpecParamMapper paramMapper;

    public List<SpecGroup> queryGroupsByCid(Long cid) {
        SpecGroup s = new SpecGroup();
        s.setCid(cid);
        List<SpecGroup> list = groupMapper.select(s);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(HttpStatus.NOT_FOUND, "该分类下没有规格组");
        }
        return list;
    }

    public List<SpecParam> queryParams(Long gid, Long cid, Boolean generic, Boolean searching) {
        SpecParam s = new SpecParam();
        s.setGroupId(gid);
        s.setCid(cid);
        s.setGeneric(generic);
        s.setSearching(searching);
        List<SpecParam> list = paramMapper.select(s);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(HttpStatus.NOT_FOUND, "该组下没有规格参数");
        }
        return list;
    }


    public List<SpecGroup> querySpecsByCid(Long cid){
        //查询规格组
        List<SpecGroup> specGroups = queryGroupsByCid(cid);
        //查询当前分类所有参数
        List<SpecParam> params = queryParams(null, cid, null, null);

        //创建map,保存参数.其key是组id,其值是组内的参数的集合
        Map<Long,List<SpecParam>> map = new HashMap<>();
        for (SpecParam param : params) {
            // 判断是否存在
            if(!map.containsKey(param.getGroupId())){
                map.put(param.getGroupId(), new ArrayList<>());
            }
            // 把param添加到list
            map.get(param.getGroupId()).add(param);
        }

        // 把group中的params赋值
        for (SpecGroup group : specGroups) {
            group.setParams(map.get(group.getId()));
        }
        return specGroups;
    }
}
