package com.infotact.warehouse.config;

import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.Warehouse;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. Ensure a System Warehouse exists for the SuperAdmin
        Warehouse systemWarehouse = warehouseRepository.findByName("System Headquarters")
                .orElseGet(() -> {
                    Warehouse warehouse = new Warehouse();
                    warehouse.setName("System Headquarters");
                    warehouse.setLocation("Cloud");
                    log.info("Seed: Created System Headquarters Warehouse.");
                    return warehouseRepository.save(warehouse);
                });

        // 2. Create the SuperAdmin (God Mode)
        if (userRepository.findByEmail("superadmin@warehouse.com").isEmpty()) {
            User superAdmin = new User();
            superAdmin.setName("Global Super Admin");
            superAdmin.setEmail("superadmin@warehouse.com");
            superAdmin.setContactNumber("0000000000");
            superAdmin.setPassword(passwordEncoder.encode("superadmin123"));
            superAdmin.setRole(Role.SUPER_ADMIN); // The high-level role
            superAdmin.setStatus(UserStatus.ACTIVE);
            superAdmin.setWarehouse(systemWarehouse);

            userRepository.save(superAdmin);

            System.out.println("*************************************************");
            System.out.println("🚀 SEEDER: Super Admin Created Successfully!");
            System.out.println("Email: superadmin@warehouse.com");
            System.out.println("Password: superadmin123");
            System.out.println("Role: SUPER_ADMIN");
            System.out.println("*************************************************");
        }

        // Optional: Create a default Local Admin for testing
        seedLocalAdmin(systemWarehouse);
    }

    private void seedLocalAdmin(Warehouse warehouse) {
        if (userRepository.findByEmail("admin@warehouse.com").isEmpty()) {
            User localAdmin = new User();
            localAdmin.setName("Local Admin");
            localAdmin.setEmail("admin@warehouse.com");
            localAdmin.setContactNumber("9999999999");
            localAdmin.setPassword(passwordEncoder.encode("admin123"));
            localAdmin.setRole(Role.ADMIN);
            localAdmin.setStatus(UserStatus.ACTIVE);
            localAdmin.setWarehouse(warehouse);
            userRepository.save(localAdmin);
            log.info("Seed: Created initial Local Admin.");
        }
    }
}