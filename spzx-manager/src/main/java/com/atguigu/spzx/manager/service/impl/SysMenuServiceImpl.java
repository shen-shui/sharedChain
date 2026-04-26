package com.atguigu.spzx.manager.service.impl;

import com.atguigu.spzx.common.service.exception.GuiguException;
import com.atguigu.spzx.manager.mapper.SysMenuMapper;
import com.atguigu.spzx.manager.mapper.SysRoleMenuMapper;
import com.atguigu.spzx.manager.service.SysMenuService;
import com.atguigu.spzx.manager.utils.MenuHelper;
import com.atguigu.spzx.model.entity.system.SysMenu;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.model.vo.system.SysMenuVo;
import com.atguigu.spzx.util.AuthContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.LinkedList;
import java.util.List;

@Service
public class SysMenuServiceImpl implements SysMenuService {

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<SysMenu> selectAll() {
        // 获取所有menu数据
        List<SysMenu> sysMenuList = sysMenuMapper.selectAll();
        if (CollectionUtils.isEmpty(sysMenuList)) return null;

        // 获取数据后要将数据格式化为Element Plus显示的格式
        return MenuHelper.buildTree(sysMenuList);
    }

    @Override
    public void save(SysMenu sysMenu) {
        sysMenuMapper.save(sysMenu);

        // 新添加一个菜单，那么此时就需要将该菜单所对应的父级菜单设置为半开
        updateSysRoleMenu(sysMenu);
    }

    private void updateSysRoleMenu(SysMenu sysMenu) {
        // 查询是否存在父节点
        SysMenu parentMenu = sysMenuMapper.selectById(sysMenu.getParentId());

        if(parentMenu != null) {
            // 将该id的菜单设置为半开
            sysRoleMenuMapper.updateSysRoleMenuIsHalf(parentMenu.getId());
            // 递归调用
            updateSysRoleMenu(parentMenu);
        }
    }

    @Override
    public void update(SysMenu sysMenu) {
        sysMenuMapper.update(sysMenu);
    }

    @Override
    public void deleteById(Long menuId) {
        // 有子菜单不逊于删除
        int count = sysMenuMapper.countByParentId(menuId);
        if (count > 0) {
            throw new GuiguException(ResultCodeEnum.NODE_ERROR);
        }
        sysMenuMapper.deleteById(menuId);
    }

    @Override
    public List<SysMenuVo> findUserMenuList() {
        Long userId = AuthContextUtil.get().getId();

        // 查询当前用户具有的菜单
        List<SysMenu> sysMenuList = sysMenuMapper.selectListByUserId(userId);
        
        // 构建树形数据
        List<SysMenu> sysMenuTreeList = MenuHelper.buildTree(sysMenuList);
        return this.buildMenus(sysMenuTreeList);
    }

    private List<SysMenuVo> buildMenus(List<SysMenu> sysMenuTreeList) {
        List<SysMenuVo> sysMenuVoList = new LinkedList<SysMenuVo>();
        for (SysMenu sysMenu : sysMenuTreeList) {
            SysMenuVo sysMenuVo = new SysMenuVo();
            sysMenuVo.setTitle(sysMenu.getTitle());
            sysMenuVo.setName(sysMenu.getComponent());
            List<SysMenu> children = sysMenu.getChildren();
            if (!CollectionUtils.isEmpty(children)) {
                sysMenuVo.setChildren(buildMenus(children));
            }
            sysMenuVoList.add(sysMenuVo);
        }
        return sysMenuVoList;
    }
}
