package com.infotact.warehouse.service;

import com.infotact.warehouse.config.TenantContext;
import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.*;
import com.infotact.warehouse.exception.BadRequestException;
import com.infotact.warehouse.exception.InsufficientStorageException;
import com.infotact.warehouse.repository.*;
import com.infotact.warehouse.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InventoryServiceImpl}.
 *
 * Tests cover the core inventory operations: receiving shipments, reserving stock,
 * releasing reservations, and committing picks. These are the most critical
 * business-logic paths in the WMS.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    @Mock
    private BinRepository binRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryItemRepository inventoryRepository;

    @Mock
    private InventoryTransactionRepository transactionRepository;

    @Mock
    private BarcodeAuditService auditService;

    @Mock
    private UserService userService;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private MockedStatic<TenantContext> tenantContextMock;

    private static final String WAREHOUSE_ID = "wh-test-001";
    private static final String PRODUCT_ID = "prod-test-001";
    private static final String BIN_ID = "bin-test-001";
    private static final String INVENTORY_ITEM_ID = "inv-test-001";

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(WAREHOUSE_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    // ============================================================
    // Helper Builders
    // ============================================================

    private Product buildProduct() {
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setSku("SKU-001");
        p.setName("Test Product");
        p.setCostPrice(new BigDecimal("10.00"));
        p.setSellingPrice(new BigDecimal("15.00"));
        p.setUnitVolume(1.0);
        p.setWeight(0.5);
        return p;
    }

    private StorageBin buildBin() {
        Warehouse wh = new Warehouse();
        wh.setId(WAREHOUSE_ID);

        Zone zone = new Zone();
        zone.setId("zone-001");
        zone.setType(ZoneType.RECEIVING);
        zone.setWarehouse(wh);

        Aisle aisle = new Aisle();
        aisle.setId("aisle-001");
        aisle.setZone(zone);

        StorageBin bin = new StorageBin();
        bin.setId(BIN_ID);
        bin.setBinCode("A-01-BIN-001");
        bin.setAisle(aisle);
        bin.setStatus(BinStatus.AVAILABLE);
        bin.setType(BinType.BULK_STORAGE);
        bin.setMaxVolume(100.0);
        bin.setMaxWeight(50.0);
        bin.setCurrentVolumeOccupied(0.0);
        bin.setCurrentWeightOccupied(0.0);
        return bin;
    }

    private InventoryItem buildInventoryItem(int qty) {
        InventoryItem item = new InventoryItem();
        item.setId(INVENTORY_ITEM_ID);
        item.setProduct(buildProduct());
        item.setStorageBin(buildBin());
        item.setQuantity(qty);
        item.setReservedQuantity(0);
        item.setStatus(InventoryStatus.AVAILABLE);
        item.setExpiryDate(LocalDate.now().plusDays(30));
        return item;
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

    // ============================================================
    // RECEIVE SHIPMENT TESTS
    // ============================================================

    @Test
    @DisplayName("receiveShipment - should successfully place stock into an available bin")
    void receiveShipment_ShouldSucceed_WhenBinAvailable() {
        // Arrange
        ReceivingRequest request = new ReceivingRequest();
        request.setProductId(PRODUCT_ID);
        request.setQuantity(10);
        request.setBatchNumber("BATCH-2025");
        request.setExpiryDate(LocalDate.now().plusDays(90));

        Product product = buildProduct();
        StorageBin bin = buildBin();

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(binRepository.findPutawayCandidates(any(), any(), anyDouble(), anyDouble(),
                any(), any(), eq(WAREHOUSE_ID), any()))
                .thenReturn(List.of(bin));
        when(binRepository.findByIdWithLock(BIN_ID, WAREHOUSE_ID)).thenReturn(Optional.of(bin));
        when(userService.getAuthenticatedUser()).thenReturn(buildOperator());
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act & Assert
        assertThatNoException().isThrownBy(() -> inventoryService.receiveShipment(request));

        verify(inventoryRepository, atLeastOnce()).save(any(InventoryItem.class));
        verify(auditService, atLeastOnce()).logSuccess(any(), any(), any(), eq(AuditAction.RECEIVING), any());
    }

    @Test
    @DisplayName("receiveShipment - should throw InsufficientStorageException when no bins available")
    void receiveShipment_ShouldThrow_WhenNoBinsAvailable() {
        // Arrange
        ReceivingRequest request = new ReceivingRequest();
        request.setProductId(PRODUCT_ID);
        request.setQuantity(10);

        Product product = buildProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(binRepository.findPutawayCandidates(any(), any(), anyDouble(), anyDouble(),
                any(), any(), eq(WAREHOUSE_ID), any()))
                .thenReturn(Collections.emptyList());
        when(userService.getAuthenticatedUser()).thenReturn(buildOperator());

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.receiveShipment(request))
                .isInstanceOf(InsufficientStorageException.class)
                .hasMessageContaining("No bins available");

        verify(inventoryRepository, never()).save(any());
    }

    // ============================================================
    // RESERVE STOCK TESTS
    // ============================================================

    @Test
    @DisplayName("reserveStock - should successfully reserve available stock and return items")
    void reserveStock_ShouldReturnReservedItems_WhenSufficientStock() {
        // Arrange
        InventoryItem item = buildInventoryItem(20);

        when(inventoryRepository.findAvailableStockForPicking(
                eq(PRODUCT_ID), eq(InventoryStatus.AVAILABLE), eq(BinType.PICK_FACE), eq(WAREHOUSE_ID)))
                .thenReturn(List.of(item));
        when(inventoryRepository.findByIdWithLock(INVENTORY_ITEM_ID, WAREHOUSE_ID))
                .thenReturn(Optional.of(item));
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        List<InventoryItem> result = inventoryService.reserveStock(PRODUCT_ID, 5);

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(item.getReservedQuantity()).isEqualTo(5);
        verify(inventoryRepository).save(item);
    }

    @Test
    @DisplayName("reserveStock - should throw BadRequestException when quantity is zero or negative")
    void reserveStock_ShouldThrow_WhenQuantityIsInvalid() {
        assertThatThrownBy(() -> inventoryService.reserveStock(PRODUCT_ID, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reservation must be positive");

        assertThatThrownBy(() -> inventoryService.reserveStock(PRODUCT_ID, -5))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(inventoryRepository);
    }

    @Test
    @DisplayName("reserveStock - should throw InsufficientStorageException when stock is too low")
    void reserveStock_ShouldThrow_WhenInsufficientStock() {
        // Arrange - only 2 units available, requesting 10
        InventoryItem item = buildInventoryItem(2);

        when(inventoryRepository.findAvailableStockForPicking(
                eq(PRODUCT_ID), eq(InventoryStatus.AVAILABLE), eq(BinType.PICK_FACE), eq(WAREHOUSE_ID)))
                .thenReturn(List.of(item));
        when(inventoryRepository.findByIdWithLock(INVENTORY_ITEM_ID, WAREHOUSE_ID))
                .thenReturn(Optional.of(item));
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Simulate no suitable bin for replenishment fallback
        when(binRepository.findBestPickingBin(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.reserveStock(PRODUCT_ID, 10))
                .isInstanceOf(InsufficientStorageException.class)
                .hasMessageContaining("Insufficient stock");
    }

    // ============================================================
    // RELEASE RESERVATION TESTS
    // ============================================================

    @Test
    @DisplayName("releaseReservation - should decrement reserved quantity correctly")
    void releaseReservation_ShouldDecrementReservedQty() {
        // Arrange
        InventoryItem item = buildInventoryItem(20);
        item.setReservedQuantity(10);

        when(inventoryRepository.findByIdWithLock(INVENTORY_ITEM_ID, WAREHOUSE_ID))
                .thenReturn(Optional.of(item));
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        inventoryService.releaseReservation(INVENTORY_ITEM_ID, 5);

        // Assert
        assertThat(item.getReservedQuantity()).isEqualTo(5);
        verify(inventoryRepository).save(item);
    }

    // ============================================================
    // COMMIT PICK TESTS
    // ============================================================

    @Test
    @DisplayName("commitPick - should deduct quantity and mark item DEPLETED when stock reaches zero")
    void commitPick_ShouldMarkDepleted_WhenQuantityBecomesZero() {
        // Arrange
        InventoryItem item = buildInventoryItem(5);
        item.setReservedQuantity(5);

        when(inventoryRepository.findByIdWithLock(INVENTORY_ITEM_ID, WAREHOUSE_ID))
                .thenReturn(Optional.of(item));
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userService.getAuthenticatedUser()).thenReturn(buildOperator());

        // Act
        inventoryService.commitPick(INVENTORY_ITEM_ID, 5);

        // Assert
        assertThat(item.getQuantity()).isEqualTo(0);
        assertThat(item.getStatus()).isEqualTo(InventoryStatus.DEPLETED);
        verify(inventoryRepository).save(item);
    }

    @Test
    @DisplayName("commitPick - should deduct correctly and leave item AVAILABLE when stock remains")
    void commitPick_ShouldRemainAvailable_WhenStockRemains() {
        // Arrange
        InventoryItem item = buildInventoryItem(20);
        item.setReservedQuantity(10);

        when(inventoryRepository.findByIdWithLock(INVENTORY_ITEM_ID, WAREHOUSE_ID))
                .thenReturn(Optional.of(item));
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userService.getAuthenticatedUser()).thenReturn(buildOperator());

        // Act
        inventoryService.commitPick(INVENTORY_ITEM_ID, 10);

        // Assert
        assertThat(item.getQuantity()).isEqualTo(10);
        assertThat(item.getStatus()).isEqualTo(InventoryStatus.AVAILABLE);
    }
}
