package com.atguigu.spzx.manager.service.impl;

import com.atguigu.spzx.manager.mapper.SysMenuMapper;
import com.atguigu.spzx.manager.mapper.SysRoleMenuMapper;
import com.atguigu.spzx.manager.service.SysMenuService;
import com.atguigu.spzx.manager.service.SysRoleMenuService;
import com.atguigu.spzx.model.dto.system.AssginMenuDto;
import com.atguigu.spzx.model.entity.system.SysMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysRoleMenuServiceImpl implements SysRoleMenuService {

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Autowired
    private SysMenuService sysMenuService;

    @Override
    public Map<String, Object> findSysRoleMenuByRoleId(Long roleId) {
        Map<String, Object> result = new HashMap<>();
        // 获取菜单列表
        List<SysMenu> sysMenuList = sysMenuService.selectAll();
        // 获取当前角色所具有的菜单列表
        List<Long> roleMenuIds = sysRoleMenuMapper.findSysRoleMenuByRoleId(roleId);

        result.put("sysMenuList", sysMenuList);
        result.put("roleMenuIds", roleMenuIds);
        return result;
    }

    @Override
    public void doAssign(AssginMenuDto assignMenuDto) {
        // 删除原来的菜单值
        sysRoleMenuMapper.deleteById(assignMenuDto.getRoleId());

        // 插入新的菜单值
        List<Map<String, Number>> menuIdList = assignMenuDto.getMenuIdList();
        if (menuIdList != null && !menuIdList.isEmpty()) {
            sysRoleMenuMapper.doAssign(assignMenuDto);
        }
    }
}
