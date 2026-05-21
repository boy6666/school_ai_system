package com.eduagent.service;

import com.eduagent.entity.Role;
import com.eduagent.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdminRoleService {

    private final RoleRepository roleRepository;

    public AdminRoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<Map<String, Object>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Role r : roles) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", r.getId());
            vo.put("roleName", r.getRoleName());
            vo.put("roleDesc", r.getRoleDesc());
            vo.put("permissions", r.getPermissions());
            vo.put("status", r.getStatus().name());
            vo.put("createTime", r.getCreateTime());
            list.add(vo);
        }
        return list;
    }

    public Map<String, Object> createRole(Map<String, Object> params) {
        String roleName = (String) params.get("roleName");
        if (roleRepository.existsByRoleName(roleName)) {
            throw new RuntimeException("角色名已存在");
        }

        Role role = new Role();
        role.setRoleName(roleName);
        role.setRoleDesc((String) params.get("roleDesc"));
        role.setPermissions((String) params.get("permissions"));
        roleRepository.save(role);

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", role.getId());
        vo.put("roleName", role.getRoleName());
        return vo;
    }

    public void updateRole(Long id, Map<String, Object> params) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        if (params.containsKey("roleName")) role.setRoleName((String) params.get("roleName"));
        if (params.containsKey("roleDesc")) role.setRoleDesc((String) params.get("roleDesc"));
        if (params.containsKey("permissions")) role.setPermissions((String) params.get("permissions"));
        if (params.containsKey("status")) role.setStatus(Role.RoleStatus.valueOf((String) params.get("status")));

        roleRepository.save(role);
    }

    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }
}
