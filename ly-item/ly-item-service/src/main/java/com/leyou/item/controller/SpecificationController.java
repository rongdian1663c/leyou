package com.leyou.item.controller;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import com.leyou.item.service.SpecificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("spec")
public class SpecificationController {

    @Autowired
    private SpecificationService specService;

    /**
     * 根据分类id查询规格参数组
     *
     */
    @GetMapping("/groups/{cid}")
    public ResponseEntity<List<SpecGroup>> queryGroupsByCid(@PathVariable("cid") Long cid) {
        return ResponseEntity.ok(specService.queryGroupsByCid(cid));
    }

    /**
     * 根据组id查询规格参数
     *
     */
    @GetMapping("params")
    public ResponseEntity<List<SpecParam>> queryParams(
            @RequestParam(value = "gid", required = false) Long gid,
            @RequestParam(value = "cid", required = false) Long cid,
            @RequestParam(value = "generic", required = false) Boolean generic,
            @RequestParam(value = "searching", required = false) Boolean searching
    ) {
        return ResponseEntity.ok(specService.queryParams(gid, cid, generic, searching));
    }

    /**
     * 根据商品分类查询规格组及组内参数
     */
    @GetMapping("{cid}")
    public ResponseEntity<List<SpecGroup>> querySpecs(@PathVariable("cid") Long cid){
        return ResponseEntity.ok(specService.querySpecsByCid(cid));
    }

}