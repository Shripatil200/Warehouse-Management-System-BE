package com.infotact.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infotact.warehouse.entity.Product;
import com.infotact.warehouse.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Collections;
import java.util.List;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.test.context.ActiveProfiles("test")
class DbInspectorTest {

    @MockBean
    private JavaMailSender javaMailSender;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.infotact.warehouse.config.JWT.JwtUtil jwtUtil;

    @Test
    void printAdminToken() {
        com.infotact.warehouse.entity.User user = new com.infotact.warehouse.entity.User();
        user.setId("d5a57429-db40-4043-97a6-2a8c34c2da37");
        user.setEmail("shri@mailinator.com");
        user.setPassword("dummy");
        user.setRole(com.infotact.warehouse.entity.enums.Role.ADMIN);
        com.infotact.warehouse.entity.Warehouse wh = new com.infotact.warehouse.entity.Warehouse();
        wh.setId("1644ca61-8bb2-4214-8c4f-fe42079c278c");
        user.setWarehouse(wh);

        com.infotact.warehouse.config.JWT.UserPrincipal principal = new com.infotact.warehouse.config.JWT.UserPrincipal(user);
        String token = jwtUtil.generateToken(principal);
        System.out.println("========== ADMIN JWT TOKEN ==========");
        System.out.println(token);
        System.out.println("=====================================");
    }

    @Test
    void inspectProducts() throws Exception {
        System.out.println("========== DB INSPECTION ==========");
        List<Product> products = productRepository.findAll();
        System.out.println("Total products in DB: " + products.size());
        for (Product p : products) {
            System.out.println("SKU: " + p.getSku() + " | Name: " + p.getName() + " | Active: " + p.isActive() + " | Warehouse: " + (p.getWarehouse() != null ? p.getWarehouse().getId() : "null"));
        }
        System.out.println("===================================");

        // Serialize a Page object to see Jackson's output format
        PageImpl<String> testPage = new PageImpl<>(Collections.singletonList("test-item"), PageRequest.of(0, 10), 24);
        String json = objectMapper.writeValueAsString(testPage);
        System.out.println("========== PAGE JSON ==========");
        System.out.println(json);
        System.out.println("===============================");
    }
}
