package com.losgai.ai.common.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.losgai.ai.entity.sys.User;
import com.losgai.ai.enums.SysRoleEnum;
import com.losgai.ai.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> list = new ArrayList<>();
        Long id = Long.valueOf((String) loginId);
        User user = userMapper.selectByPrimaryKey(id);
        if (user.getRole() == SysRoleEnum.ADMIN.getCode().longValue()) { // 根据角色来返回权限
            list.add("admin.*");
        } else if (user.getRole() == SysRoleEnum.USER.getCode().longValue()) {
            list.add("user.*");
        }
        return list;
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 本 list 仅做模拟，实际项目中要根据具体业务逻辑来查询权限
        List<String> list = new ArrayList<>();
        Long id = Long.valueOf((String) loginId);
        User user = userMapper.selectByPrimaryKey(id);
        if (user.getRole() == SysRoleEnum.ADMIN.getCode().longValue()) { // 根据角色来返回权限
            list.add(SysRoleEnum.ADMIN.getMessage());
        } else if (user.getRole() == SysRoleEnum.USER.getCode().longValue()) {
            list.add(SysRoleEnum.USER.getMessage());
        }
        return list;
    }

}