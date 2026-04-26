package com.atguigu.spzx.manager.controller;

import com.atguigu.spzx.common.log.annotation.Log;
import com.atguigu.spzx.common.log.enums.OperatorType;
import com.atguigu.spzx.manager.service.SysRoleService;
import com.atguigu.spzx.model.dto.system.SysRoleDto;
import com.atguigu.spzx.model.entity.system.SysRole;
import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/admin/system/sysRole")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    // 角色列表的方法
    @PostMapping("/findByPage/{current}/{limit}")
    public Result findByPage(@PathVariable("current") Integer current,
                             @PathVariable("limit") Integer limit,
                             @RequestBody SysRoleDto sysRoleDto) {
        PageInfo<SysRole> pageInfo = sysRoleService.findByPage(current, limit, sysRoleDto);
        return Result.build(pageInfo, ResultCodeEnum.SUCCESS);
    }

    // 添加角色
    @Log(title = "增加角色", businessType = 0, operatorType = OperatorType.MANAGE)
    @PostMapping("/saveSysRole")
    public Result saveSysRole(@RequestBody SysRole sysRole) {
        sysRoleService.saveSysRole(sysRole);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    // 修改角色
    @PutMapping("/updateSysRole")
    public Result updateSysRole(@RequestBody SysRole sysRole) {
        sysRoleService.updateSysRole(sysRole);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    // 删除角色
    @DeleteMapping("/deleteById/{roleId}")
    public Result deleteById(@PathVariable("roleId") Long id) {
        sysRoleService.deleteById(id);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    // 获取所有角色
    @GetMapping("/findAllRoles/{userId}")
    public Result findAllRoles(@PathVariable("userId") Long userId) {
        Map<String, Object> map = sysRoleService.findAll(userId);
        return Result.build(map, ResultCodeEnum.SUCCESS);
    }
}
