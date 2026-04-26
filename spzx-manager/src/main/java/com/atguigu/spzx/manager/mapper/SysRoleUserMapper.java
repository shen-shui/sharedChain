package com.atguigu.spzx.manager.mapper;

import com.atguigu.spzx.model.dto.system.AssginRoleDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysRoleUserMapper {
    void deleteByUserId(Long userId);

    void doAssignBatch(AssginRoleDto assignRoleDto);

    List<Long> findSysUserRoleByUserId(Long userId);
}
