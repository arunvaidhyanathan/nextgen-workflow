package com.citi.onecms.repository;

import com.citi.onecms.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleId(Long roleId);
    Optional<Role> findByRoleCode(String roleCode);
    List<Role> findByIsActiveTrue();
    List<Role> findByAccessLevel(String accessLevel);
    boolean existsByRoleCode(String roleCode);
}