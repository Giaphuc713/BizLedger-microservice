package com.overcode250204.identityservice.repository;

import com.overcode250204.identityservice.entity.UserRole;
import com.overcode250204.identityservice.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    /** Assigns a role to a user (idempotent). */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO user_role(user_id, role_id) VALUES (:userId, :roleId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void assignRoleToUser(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    /** Assigns a permission to a role (idempotent). */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO role_permission(role_id, permission_id) VALUES (:roleId, :permissionId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void assignPermissionToRole(@Param("roleId") UUID roleId, @Param("permissionId") UUID permissionId);

    /** Returns all role codes assigned to a user. */
    @Query("SELECT r.code FROM UserRole ur JOIN Role r ON ur.id.roleId = r.id WHERE ur.id.userId = :userId ORDER BY r.code")
    List<String> findRoleCodesByUserId(@Param("userId") UUID userId);

    /**
     * Returns all distinct permission codes assigned to a user (via their roles).
     */
    @Query("SELECT DISTINCT p.code FROM UserRole ur " +
            "JOIN RolePermission rp ON rp.id.roleId = ur.id.roleId " +
            "JOIN Permission p ON p.id = rp.id.permissionId " +
            "WHERE ur.id.userId = :userId " +
            "ORDER BY p.code")
    List<String> findPermissionCodesByUserId(@Param("userId") UUID userId);

    /** Returns all permission codes assigned to a specific role. */
    @Query("SELECT p.code FROM RolePermission rp " +
            "JOIN Permission p ON p.id = rp.id.permissionId " +
            "WHERE rp.id.roleId = :roleId " +
            "ORDER BY p.code")
    List<String> findPermissionCodesByRoleId(@Param("roleId") UUID roleId);

    /** Deletes all permissions associated with a role. */
    @Modifying
    @Transactional
    @Query("DELETE FROM RolePermission rp WHERE rp.id.roleId = :roleId")
    void deletePermissionsForRole(@Param("roleId") UUID roleId);
}
