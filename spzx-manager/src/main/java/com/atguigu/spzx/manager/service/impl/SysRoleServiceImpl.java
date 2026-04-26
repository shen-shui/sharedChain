package com.atguigu.spzx.manager.service.impl;

import com.atguigu.spzx.manager.mapper.SysRoleMapper;
import com.atguigu.spzx.manager.mapper.SysRoleUserMapper;
import com.atguigu.spzx.manager.service.SysRoleService;
import com.atguigu.spzx.model.dto.system.SysRoleDto;
import com.atguigu.spzx.model.entity.system.SysRole;
import com.atguigu.spzx.model.entity.system.SysRoleUser;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysRoleServiceImpl implements SysRoleService {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysRoleUserMapper sysRoleUserMapper;

    @Override
    public PageInfo<SysRole> findByPage(Integer current, Integer limit, SysRoleDto sysRoleDto) {
        // 设置分页参数
        PageHelper.startPage(current, limit);
        // 根据条件查询查询所有数据
        List<SysRole> list = sysRoleMapper.findByPage(sysRoleDto);
        // 封装PageInfo对象
        PageInfo<SysRole> pageInfo = new PageInfo<>(list);
        return pageInfo;
    }

    @Override
    public void saveSysRole(SysRole sysRole) {
        sysRoleMapper.saveSysRole(sysRole);
    }

    @Override
    public void updateSysRole(SysRole sysRole) {
        sysRoleMapper.updateSysRole(sysRole);
    }

    @Override
    public void deleteById(Long id) {
        sysRoleMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> findAll(Long userId) {
        Map<String, Object> resultMap = new HashMap<>();
        List<SysRole> sysRoleList = sysRoleMapper.findAll();
        List<Long> sysRoles = sysRoleUserMapper.findSysUserRoleByUserId(userId);
        resultMap.put("allRolesList", sysRoleList);
        resultMap.put("sysUserRoles", sysRoles);
        return resultMap;
    }
}
