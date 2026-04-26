package com.atguigu.spzx.manager.controller;

import com.atguigu.spzx.manager.service.SysMenuService;
import com.atguigu.spzx.model.entity.system.SysMenu;
import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/admin/system/sysMenu")
public class SysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    @GetMapping("/findNodes")
    public Result findNodes() {
        List<SysMenu> sysMenuList = sysMenuService.selectAll();
        return Result.build(sysMenuList, ResultCodeEnum.SUCCESS);
    }

    @PostMapping("/save")
    public Result save(@RequestBody SysMenu sysMenu) {
        sysMenuService.save(sysMenu);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @PutMapping("/update")
    public Result update(@RequestBody SysMenu sysMenu) {
        sysMenuService.update(sysMenu);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @DeleteMapping("/deleteById/{menuId}")
    public Result deleteById(@PathVariable("menuId") Long menuId) {
        sysMenuService.deleteById(menuId);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }
}
