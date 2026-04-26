package com.atguigu.spzx.manager.service;

import com.atguigu.spzx.model.dto.system.SysRoleDto;
import com.atguigu.spzx.model.entity.system.SysRole;
import com.github.pagehelper.PageInfo;

import java.util.Map;

public interface SysRoleService {
    PageInfo<SysRole> findByPage(Integer current, Integer limit, SysRoleDto sysRoleDto);

    void saveSysRole(SysRole sysRole);

    void updateSysRole(SysRole sysRole);

    void deleteById(Long id);

    Map<String, Object> findAll(Long userId);
}
