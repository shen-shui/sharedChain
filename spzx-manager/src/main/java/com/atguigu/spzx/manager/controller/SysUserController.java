package com.atguigu.spzx.manager.controller;

import com.atguigu.spzx.common.log.annotation.Log;
import com.atguigu.spzx.manager.service.SysUserService;
import com.atguigu.spzx.model.dto.system.AssginRoleDto;
import com.atguigu.spzx.model.dto.system.SysUserDto;
import com.atguigu.spzx.model.entity.system.SysUser;
import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/system/sysUser")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    // 用户列表的方法
    @GetMapping("/findByPage/{current}/{limit}")
    public Result findByPage(@PathVariable("current") Integer current,
                             @PathVariable("limit") Integer limit,
                             SysUserDto sysUserDto) {
        PageInfo<SysUser> pageInfo = sysUserService.findByPage(current, limit, sysUserDto);
        return Result.build(pageInfo, ResultCodeEnum.SUCCESS);
    }

    // 添加用户
    @Log(title = "角色添加",businessType = 0)
    @PostMapping("/saveSysUser")
    public Result saveSysUser(@RequestBody SysUser sysUser) {
        sysUserService.saveSysUser(sysUser);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    // 修改用户
    @PutMapping("/updateSysUser")
    public Result updateSysUser(@RequestBody SysUser sysUser) {
        sysUserService.updateSysUser(sysUser);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    // 删除用户
    @DeleteMapping("/deleteById/{userId}")
    public Result deleteById(@PathVariable("userId") Long userId) {
        sysUserService.deleteById(userId);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    // 添加用户角色
    @PostMapping("/doAssign")
    public Result doAssign(@RequestBody AssginRoleDto assignRoleDto) {
        sysUserService.doAssign(assignRoleDto);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }
}
