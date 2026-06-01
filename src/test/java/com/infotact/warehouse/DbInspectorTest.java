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
