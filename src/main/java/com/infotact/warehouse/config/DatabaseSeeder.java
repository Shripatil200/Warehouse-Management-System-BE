package com.infotact.warehouse.config;

import com.infotact.warehouse.entity.Warehouse;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.UserStatus;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component // This tells Spring to manage this class
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WarehouseRepository warehouseRepository;


    @Override
    public void run(String... args) throws Exception {
        // Only run if the database is empty to avoid duplicates
        if (userRepository.count() == 0) {
            Warehouse warehouse = warehouseRepository.findById("d5994664-3efe-4fa9-a85e-485dcbc294d2").orElseThrow(()-> new ResourceNotFoundException("Warehouse not found"));
            User admin = new User();
            // admin.setId("admin-001"); // Only if your ID is not auto-generated
            admin.setName("System Admin");
            admin.setEmail("admin@warehouse.com");
            admin.setPassword(passwordEncoder.encode("admin123")); // Your password
            admin.setRole(Role.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setContactNumber("9999999999");
            admin.setWarehouse(warehouse);
            userRepository.save(admin);

            System.out.println("*************************************************");
            System.out.println("✅ SEEDER: Initial Admin created successfully!");
            System.out.println("Username: admin@warehouse.com | Password: admin123");
            System.out.println("*************************************************");
        }
    }
}