package com.atguigu.spzx.manager.mapper;

import com.atguigu.spzx.model.dto.system.AssginMenuDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysRoleMenuMapper {
    List<Long> findSysRoleMenuByRoleId(Long roleId);

    void deleteById(Long roleId);

    void doAssign(AssginMenuDto assignMenuDto);

    void updateSysRoleMenuIsHalf(Long id);
}
