package com.atguigu.spzx.manager.service;

import com.atguigu.spzx.model.entity.system.SysMenu;
import com.atguigu.spzx.model.vo.system.SysMenuVo;

import java.util.List;

public interface SysMenuService {
    List<SysMenu> selectAll();

    void save(SysMenu sysMenu);

    void update(SysMenu sysMenu);

    void deleteById(Long menuId);

    List<SysMenuVo> findUserMenuList();
}
