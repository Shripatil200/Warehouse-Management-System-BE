package com.infotact.warehouse.service;

import com.infotact.warehouse.config.WarehouseContext;
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

/**
 * Unit tests for {@link InventoryServiceImpl}.
 *
 * <p>Uses Mockito's {@code @Mock} for {@link WarehouseContext} — the warehouse ID
 * is read from the Spring Security context via an instance method, not a static call.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    @Mock private BinRepository              binRepository;
    @Mock private ProductRepository          productRepository;
    @Mock private InventoryRepository        inventoryRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private BarcodeAuditService        auditService;
    @Mock private UserService                userService;
    @Mock private WarehouseContext           warehouseContext;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private static final String WAREHOUSE_ID = "wh-test-001";
    private static final String PRODUCT_ID   = "prod-test-001";
    private static final String BIN_ID       = "bin-test-001";

    @BeforeEach
    void setUp() {
        // intentionally empty — stubs are set per-test to avoid unnecessary stubbing errors
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("receiveShipment - should place stock into an available bin successfully")
    void receiveShipment_ShouldSucceed_WhenBinAvailable() {
        ReceivingRequest request = new ReceivingRequest();
        request.setProductId(PRODUCT_ID);
        request.setQuantity(10);
        request.setBatchNumber("BATCH-2025");
        request.setExpiryDate(LocalDate.now().plusDays(90));

        StorageBin bin     = buildBin();
        Product    product = buildProduct();

        when(warehouseContext.getWarehouseId()).thenReturn(WAREHOUSE_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(userService.getAuthenticatedUser()).thenReturn(buildOperator());
        when(binRepository.findPutawayCandidates(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(bin));
        when(binRepository.findByIdWithLock(BIN_ID, WAREHOUSE_ID)).thenReturn(Optional.of(bin));
        when(inventoryRepository
                .findByProductIdAndStorageBinIdAndBatchNumberAndPurchasePriceAndExpiryDateAndStorageBin_Warehouse_Id(
                        eq(PRODUCT_ID), eq(BIN_ID), eq("BATCH-2025"),
                        any(BigDecimal.class), any(LocalDate.class), eq(WAREHOUSE_ID)))
                .thenReturn(Optional.empty());
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(inv -> {
            InventoryItem item = inv.getArgument(0);
            item.setId("inv-" + System.nanoTime());
            return item;
        });
        when(binRepository.save(any(StorageBin.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThatNoException().isThrownBy(() -> inventoryService.receiveShipment(request));
        verify(inventoryRepository, atLeastOnce()).save(any(InventoryItem.class));
    }

    @Test
    @DisplayName("receiveShipment - should throw InsufficientStorageException when no bins available")
    void receiveShipment_ShouldThrow_WhenNoBinsAvailable() {
        ReceivingRequest request = new ReceivingRequest();
        request.setProductId(PRODUCT_ID);
        request.setQuantity(10);

        when(warehouseContext.getWarehouseId()).thenReturn(WAREHOUSE_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(buildProduct()));
        when(userService.getAuthenticatedUser()).thenReturn(buildOperator());
        when(binRepository.findPutawayCandidates(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

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