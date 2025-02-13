package com.dxvalley.crowdfunding.controller;

import com.dxvalley.crowdfunding.model.Role;
import com.dxvalley.crowdfunding.repository.RoleRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/Roles")
public class RoleController {
    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping("/GetRole/{roleName}")
    Role getRoleByName (@PathVariable String roleName) {
        return roleRepository.findByRoleName(roleName);
    }

    @GetMapping("/GetRoles")
    List<Role> getRoles () {
        return roleRepository.findAll();
    }
}
