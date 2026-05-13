package com.infotact.warehouse.service;

import com.infotact.warehouse.config.TenantContext;
import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.*;
import com.infotact.warehouse.exception.BadRequestException;
import com.infotact.warehouse.exception.InsufficientStorageException;
import com.infotact.warehouse.repository.*;
import com.infotact.warehouse.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    @Mock private BinRepository binRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private BarcodeAuditService auditService;
    @Mock private UserService userService;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private MockedStatic<TenantContext> tenantContextMock;

    private static final String WAREHOUSE_ID = "wh-test-001";
    private static final String PRODUCT_ID   = "prod-test-001";
    private static final String BIN_ID       = "bin-test-001";

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::get).thenReturn(WAREHOUSE_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    private Product buildProduct() {
        Warehouse wh = new Warehouse();
        wh.setId(WAREHOUSE_ID);
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setSku("SKU-001");
        p.setName("Test Product");
        p.setCostPrice(new BigDecimal("10.00"));
        p.setSellingPrice(new BigDecimal("15.00"));
        p.setUnitVolume(new BigDecimal("1.0"));
        p.setWeight(0.5);
        p.setWarehouse(wh);
        return p;
    }

    private StorageBin buildBin() {
        Warehouse wh = new Warehouse();
        wh.setId(WAREHOUSE_ID);
        Zone zone = new Zone();
        zone.setId("zone-001");
        zone.setWarehouse(wh);
        Aisle aisle = new Aisle();
        aisle.setId("aisle-001");
        aisle.setZone(zone);
        StorageBin bin = new StorageBin();
        bin.setId(BIN_ID);
        bin.setBinCode("A-01-BIN-001");
        bin.setAisle(aisle);
        bin.setWarehouse(wh);
        bin.setStatus(BinStatus.AVAILABLE);
        bin.setBinType(BinType.BULK_STORAGE);
        bin.setMaxVolume(100.0);
        bin.setMaxWeightCapacity(50.0);
        bin.setCurrentVolumeOccupied(0.0);
        bin.setCurrentWeightLoad(0.0);
        return bin;
    }

    private User buildOperator() {
        Warehouse wh = new Warehouse();
        wh.setId(WAREHOUSE_ID);
        User u = new User();
        u.setId("user-001");
        u.setEmail("operator@test.com");
        u.setWarehouse(wh);
        return u;
    }

    @Test
    @DisplayName("receiveShipment - should place stock into an available bin successfully")
    void receiveShipment_ShouldSucceed_WhenBinAvailable() {
        // 1. Arrange Data
        ReceivingRequest request = new ReceivingRequest();
        request.setProductId(PRODUCT_ID);
        request.setQuantity(10);
        request.setBatchNumber("BATCH-2025");
        request.setExpiryDate(LocalDate.now().plusDays(90));

        StorageBin bin = buildBin();
        Product product = buildProduct();

        // 2. Mocking Behaviors
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(userService.getAuthenticatedUser()).thenReturn(buildOperator());

        // Mock Bin Search
        when(binRepository.findPutawayCandidates(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(bin));
        when(binRepository.findByIdWithLock(BIN_ID, WAREHOUSE_ID)).thenReturn(Optional.of(bin));

        // FIX FOR POINT 2 & 3: Stub the long search query
        // We use specific matchers to ensure the 6-argument method is matched correctly
        when(inventoryRepository.findByProductIdAndStorageBinIdAndBatchNumberAndPurchasePriceAndExpiryDateAndStorageBin_Warehouse_Id(
                eq(PRODUCT_ID),
                eq(BIN_ID),
                eq("BATCH-2025"),
                any(BigDecimal.class),
                any(LocalDate.class),
                eq(WAREHOUSE_ID)
        )).thenReturn(Optional.empty());

        // Mock the Save
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(i -> {
            InventoryItem item = i.getArgument(0);
            item.setId("inv-" + System.nanoTime());
            return item;
        });

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // 3. Act & Assert
        assertThatNoException().isThrownBy(() -> inventoryService.receiveShipment(request));

        verify(inventoryRepository, atLeastOnce()).save(any(InventoryItem.class));
    }

    @Test
    @DisplayName("receiveShipment - should throw InsufficientStorageException when no bins available")
    void receiveShipment_ShouldThrow_WhenNoBinsAvailable() {
        ReceivingRequest request = new ReceivingRequest();
        request.setProductId(PRODUCT_ID);
        request.setQuantity(10);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(buildProduct()));
        when(binRepository.findPutawayCandidates(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(userService.getAuthenticatedUser()).thenReturn(buildOperator());

        assertThatThrownBy(() -> inventoryService.receiveShipment(request))
                .isInstanceOf(InsufficientStorageException.class);

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("reserveStock - should throw BadRequestException for zero or negative quantity")
    void reserveStock_ShouldThrow_WhenQuantityInvalid() {
        assertThatThrownBy(() -> inventoryService.reserveStock(PRODUCT_ID, 0))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> inventoryService.reserveStock(PRODUCT_ID, -3))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(inventoryRepository);
    }
}