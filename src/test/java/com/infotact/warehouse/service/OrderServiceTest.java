package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.OrderRequest;
import com.infotact.warehouse.dto.v1.response.OrderResponse;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.OrderStatus;
import com.infotact.warehouse.exception.IllegalOperationException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.OrderRepository;
import com.infotact.warehouse.repository.ProductRepository;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.argThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private InventoryService inventoryService;
    @Mock private LayoutService layoutService;
    @Mock private BarcodeAuditService auditService;
    @Mock private ConsignmentService consignmentService;
    @Mock private TaskAssignmentService taskEngine; // Safely wired by @InjectMocks

    @InjectMocks
    private OrderServiceImpl orderService;

    private static final String WAREHOUSE_ID = "wh-test-001";
    private static final String ORDER_ID     = "order-test-001";
    private static final String PRODUCT_ID   = "prod-test-001";
    private static final String USER_EMAIL   = "manager@wms.com";

    @BeforeEach
    void setUpSecurityContext() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(USER_EMAIL);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private User buildManager() {
        Warehouse wh = new Warehouse();
        wh.setId(WAREHOUSE_ID);
        wh.setName("Main Warehouse");
        User u = new User();
        u.setId("user-001");
        u.setEmail(USER_EMAIL);
        u.setWarehouse(wh);
        return u;
    }

    private Product buildProduct() {
        Warehouse wh = new Warehouse();
        wh.setId(WAREHOUSE_ID);
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setSku("SKU-001");
        p.setName("Widget");
        p.setSellingPrice(new BigDecimal("25.00"));
        p.setCostPrice(new BigDecimal("15.00"));
        p.setWarehouse(wh);
        return p;
    }

    private InventoryItem buildInventoryItem() {
        StorageBin bin = new StorageBin();
        bin.setId("bin-001");
        bin.setBinCode("A-01-001");
        InventoryItem item = new InventoryItem();
        item.setId("inv-001");
        item.setStorageBin(bin);
        return item;
    }

    private SellingOrder buildOrder(OrderStatus status) {
        Warehouse wh = new Warehouse();
        wh.setId(WAREHOUSE_ID);
        wh.setName("Main Warehouse");
        SellingOrder order = new SellingOrder();
        order.setId(ORDER_ID);
        order.setOrderNumber("ORD-2025-001");
        order.setStatus(status);
        order.setWarehouse(wh);
        order.setItems(List.of());
        return order;
    }

    @Test
    @DisplayName("createOrder - should persist order with PENDING status")
    void createOrder_ShouldReturnPendingOrder() {
        OrderRequest.OrderItemRequest itemReq = new OrderRequest.OrderItemRequest();
        itemReq.setSku("SKU-001");
        itemReq.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setOrderNumber("ORD-2025-001");
        request.setItems(List.of(itemReq));

        // Stubbing data layer and infrastructure behaviors
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(buildManager()));
        when(productRepository.findBySkuAndWarehouseIdAndActiveTrue("SKU-001", WAREHOUSE_ID))
                .thenReturn(Optional.of(buildProduct()));
        when(inventoryService.reserveStock(PRODUCT_ID, 2)).thenReturn(List.of(buildInventoryItem()));
        when(orderRepository.save(any(SellingOrder.class))).thenAnswer(inv -> {
            SellingOrder o = inv.getArgument(0);
            o.setId(ORDER_ID);
            return o;
        });
        when(layoutService.getBinCodeById(anyString())).thenReturn("A-01-001");

        // Stubbing taskEngine to avoid deep return NullPointerExceptions during order post-processing hooks
        when(taskEngine.createPickingTask(any(SellingOrder.class), anyString(), eq(WAREHOUSE_ID)))
                .thenReturn(new Task());

        // Execution
        OrderResponse response = orderService.createOrder(request);

        // Verifications
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(any(SellingOrder.class));
        verify(inventoryService).reserveStock(PRODUCT_ID, 2);

        // Verifying that the task engine pipeline was triggered with correct parameters
        verify(taskEngine).createPickingTask(any(SellingOrder.class), anyString(), eq(WAREHOUSE_ID));
    }

    @Test
    @DisplayName("createOrder - should throw ResourceNotFoundException for unknown SKU")
    void createOrder_ShouldThrow_WhenSkuNotFound() {
        OrderRequest.OrderItemRequest itemReq = new OrderRequest.OrderItemRequest();
        itemReq.setSku("UNKNOWN-SKU");
        itemReq.setQuantity(1);

        OrderRequest request = new OrderRequest();
        request.setOrderNumber("ORD-BAD");
        request.setItems(List.of(itemReq));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(buildManager()));
        when(productRepository.findBySkuAndWarehouseIdAndActiveTrue(eq("UNKNOWN-SKU"), eq(WAREHOUSE_ID)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(taskEngine); // Task engine shouldn't be touched if validation fails
    }

    @Test
    @DisplayName("updateOrderStatus - PACKED directly should require scan endpoint")
    void updateStatus_DirectlyToPacked_ShouldThrow() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(buildOrder(OrderStatus.PICKING)));

        assertThatThrownBy(() -> orderService.updateOrderStatus(ORDER_ID, OrderStatus.PACKED))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("verify-pack");
    }

    @Test
    @DisplayName("updateOrderStatus - CANCELLED should release all inventory reservations")
    void updateStatus_Cancelled_ShouldReleaseInventory() {
        SellingOrderItem item = new SellingOrderItem();
        item.setInventoryItemId("inv-001");
        item.setQuantity(3);
        item.setProduct(buildProduct());
        item.setSellPriceAtTimeOfOrder(new BigDecimal("25.00"));

        SellingOrder order = buildOrder(OrderStatus.PENDING);
        order.setItems(List.of(item));

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(buildManager()));
        when(layoutService.getBinCodeById(any())).thenReturn("A-01-001");

        orderService.updateOrderStatus(ORDER_ID, OrderStatus.CANCELLED);

        verify(inventoryService).releaseReservation("inv-001", 3);
    }

    @Test
    @DisplayName("getOrder - should throw when order belongs to different warehouse")
    void getOrder_ShouldThrow_WhenWarehouseMismatch() {
        Warehouse other = new Warehouse();
        other.setId("wh-different");
        other.setName("Other");

        SellingOrder order = buildOrder(OrderStatus.PENDING);
        order.setWarehouse(other);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(buildManager()));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrder(ORDER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── verifyAndPack tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("verifyAndPack - should throw IllegalOperationException when scanned SKU doesn't match item")
    void verifyAndPack_ShouldThrow_WhenSkuMismatch() {
        Product product = buildProduct(); // SKU = "SKU-001"

        SellingOrderItem item = new SellingOrderItem();
        item.setInventoryItemId("inv-001");
        item.setQuantity(1);
        item.setProduct(product);
        item.setSuggestedBinId("bin-001");
        item.setSellPriceAtTimeOfOrder(product.getSellingPrice());

        SellingOrder order = buildOrder(OrderStatus.PICKING);
        order.setItems(List.of(item));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(buildManager()));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        // Scanned SKU deliberately wrong
        assertThatThrownBy(() ->
                orderService.verifyAndPack(ORDER_ID, "inv-001", "WRONG-SKU", "A-01-001", 1))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("Mismatch");

        // Audit failure must be recorded and inventory must NOT be committed
        verify(auditService).logFailure(any(), any(), any(), any(), any(), any());
        verify(inventoryService, never()).commitPickWithVerification(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("verifyAndPack - should throw IllegalOperationException when bin code doesn't match")
    void verifyAndPack_ShouldThrow_WhenBinMismatch() {
        Product product = buildProduct();

        SellingOrderItem item = new SellingOrderItem();
        item.setInventoryItemId("inv-001");
        item.setQuantity(1);
        item.setProduct(product);
        item.setSuggestedBinId("bin-001");
        item.setSellPriceAtTimeOfOrder(product.getSellingPrice());

        SellingOrder order = buildOrder(OrderStatus.PICKING);
        order.setItems(List.of(item));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(buildManager()));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        // SKU matches but bin scan returns false (wrong bin)
        when(layoutService.verifyBinScan(eq("WRONG-BIN"), eq("bin-001"))).thenReturn(false);

        assertThatThrownBy(() ->
                orderService.verifyAndPack(ORDER_ID, "inv-001", "SKU-001", "WRONG-BIN", 1))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("Location Error");

        verify(auditService).logFailure(any(), any(), any(), any(), any(), any());
        verify(inventoryService, never()).commitPickWithVerification(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("verifyAndPack - should advance order to PACKED when all items are picked")
    void verifyAndPack_ShouldAdvanceToPacked_WhenAllItemsPicked() {
        Product product = buildProduct();

        SellingOrderItem item = new SellingOrderItem();
        item.setInventoryItemId("inv-001");
        item.setQuantity(2);
        item.setProduct(product);
        item.setSuggestedBinId("bin-001");
        item.setSellPriceAtTimeOfOrder(product.getSellingPrice());
        item.setConsignment(false);

        SellingOrder order = buildOrder(OrderStatus.PICKING);
        order.setItems(List.of(item));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(buildManager()));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(layoutService.verifyBinScan(eq("A-01-001"), eq("bin-001"))).thenReturn(true);
        // After scanning the only item, it counts as fully picked
        when(inventoryService.isFullyPicked("inv-001")).thenReturn(true);
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        orderService.verifyAndPack(ORDER_ID, "inv-001", "SKU-001", "A-01-001", 2);

        verify(inventoryService).commitPickWithVerification("inv-001", "A-01-001", "SKU-001", 2);
        verify(auditService).logSuccess(any(), any(), any(), any(), any());
        // Verify the order was saved with PACKED status
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PACKED));
    }

    @Test
    @DisplayName("verifyAndPack - should set order to PICKING when not all items are scanned yet")
    void verifyAndPack_ShouldRemainPicking_WhenMoreItemsPending() {
        Product product = buildProduct();

        SellingOrderItem item1 = new SellingOrderItem();
        item1.setInventoryItemId("inv-001");
        item1.setQuantity(1);
        item1.setProduct(product);
        item1.setSuggestedBinId("bin-001");
        item1.setSellPriceAtTimeOfOrder(product.getSellingPrice());
        item1.setConsignment(false);

        SellingOrderItem item2 = new SellingOrderItem();
        item2.setInventoryItemId("inv-002");
        item2.setQuantity(1);
        item2.setProduct(product);
        item2.setSuggestedBinId("bin-002");
        item2.setSellPriceAtTimeOfOrder(product.getSellingPrice());
        item2.setConsignment(false);

        SellingOrder order = buildOrder(OrderStatus.PICKING);
        order.setItems(List.of(item1, item2));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(buildManager()));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(layoutService.verifyBinScan(eq("A-01-001"), eq("bin-001"))).thenReturn(true);
        // item2 is NOT yet picked
        when(inventoryService.isFullyPicked("inv-002")).thenReturn(false);
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        orderService.verifyAndPack(ORDER_ID, "inv-001", "SKU-001", "A-01-001", 1);

        // Status must stay PICKING, not jump to PACKED
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PICKING));
    }
}