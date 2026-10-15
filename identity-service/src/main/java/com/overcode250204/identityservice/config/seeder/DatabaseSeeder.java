package com.overcode250204.identityservice.config.seeder;

import com.overcode250204.identityservice.entity.*;
import com.overcode250204.identityservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Java database seeding...");

        // 1. Seed Tenant
        UUID tenantId = UUID.fromString("d3b07384-d113-4c92-a1b6-d3aef6d9ef59");
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseGet(() -> {
                    log.info("Seeding default tenant 'Northwind Traders'");
                    return tenantRepository.save(new Tenant(
                            tenantId,
                            "Northwind Traders",
                            "tn_bizledger",
                            "ACTIVE",
                            OffsetDateTime.now()));
                });

        // 2. Seed Owner Role
        UUID roleId = UUID.fromString("8f26df80-a61c-4b5c-8977-9be7bd39ca5a");
        Role ownerRole = roleRepository.findById(roleId)
                .orElseGet(() -> {
                    log.info("Seeding default Owner role");
                    return roleRepository.save(new Role(
                            roleId,
                            tenantId,
                            "role_owner",
                            "Owner"));
                });

        // 3. Seed Owner User
        UUID userId = UUID.fromString("c04e223b-09bb-4c28-971a-28952cc29352");
        User ownerUser = userRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("Seeding default owner user minh.tran@bizledger.io");
                    return userRepository.save(new User(
                            userId,
                            tenantId,
                            "minh.tran@bizledger.io",
                            passwordEncoder.encode("password123"),
                            "Minh Tran",
                            true,
                            OffsetDateTime.now()));
                });

        // 4. Map Owner User to Owner Role
        log.info("Assigning Owner role to Owner user");
        userRoleRepository.assignRoleToUser(ownerUser.getId(), ownerRole.getId());

        // 5. Map all permissions to Owner Role
        log.info("Mapping all system permissions to Owner role");
        List<Permission> permissions = permissionRepository.findAll();
        if (permissions.isEmpty()) {
            log.warn("No system permissions found in database to map to Owner role.");
        } else {
            for (Permission permission : permissions) {
                userRoleRepository.assignPermissionToRole(ownerRole.getId(), permission.getId());
            }
        }

        log.info("Java database seeding completed successfully.");
    }
}
