package com.atguigu.spzx.manager.utils;

import com.atguigu.spzx.model.entity.system.SysMenu;

import java.util.ArrayList;
import java.util.List;

public class MenuHelper {

    public static List<SysMenu> buildTree(List<SysMenu> sysMenuList) {
        // 进行递归遍历
        List<SysMenu> treeList = new ArrayList<>();
        for (SysMenu sysMenu : sysMenuList) {
            if (sysMenu.getParentId() == 0) {
                treeList.add(findChildren(sysMenu, sysMenuList));
            }
        }

        return treeList;
    }

    private static SysMenu findChildren(SysMenu sysMenu, List<SysMenu> sysMenuList) {
        sysMenu.setChildren(new ArrayList<>());
        for (SysMenu menu : sysMenuList) {
            if (menu.getParentId().longValue() == sysMenu.getId().longValue()) {
                sysMenu.getChildren().add(findChildren(menu, sysMenuList));
            }
        }

        return sysMenu;
    }
}
