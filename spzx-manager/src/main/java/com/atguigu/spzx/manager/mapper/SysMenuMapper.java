package com.atguigu.spzx.manager.mapper;

import com.atguigu.spzx.model.entity.system.SysMenu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysMenuMapper {
    List<SysMenu> selectAll();

    void save(SysMenu sysMenu);

    void update(SysMenu sysMenu);

    int countByParentId(Long menuId);

    void deleteById(Long menuId);

    SysMenu selectById(Long parentId);

    List<SysMenu> selectListByUserId(Long userId);
}
