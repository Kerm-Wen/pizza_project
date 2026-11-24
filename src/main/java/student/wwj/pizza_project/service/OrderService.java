package student.wwj.pizza_project.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import student.wwj.pizza_project.dto.OrderRequest;
import student.wwj.pizza_project.entity.Member;
import student.wwj.pizza_project.entity.OrderItem;
import student.wwj.pizza_project.entity.Orders;
import student.wwj.pizza_project.entity.Product;
import student.wwj.pizza_project.repository.OrderItemRepository;
import student.wwj.pizza_project.repository.OrderRepository;
import student.wwj.pizza_project.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private HttpSession session;

    // 1. 查詢所有訂單（並帶出每筆訂單的購買明細）
    public List<Orders> getAllOrders() {
        List<Orders> orders = orderRepository.findAll();
        for (Orders order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            order.setItems(items);
        }
        return orders;
    }

    // 2. 根據 ID 查詢單一主訂單
    public Orders getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    // 3. 批次建立訂單與扣庫存（主檔 + 明細檔架構）
    @Transactional
    public Orders createOrder(OrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("購物車是空的！");
        }

        // 外帶/外送驗證防呆
        String dType = request.getDeliveryType() != null ? request.getDeliveryType() : "TAKEOUT";
        if ("DELIVERY".equals(dType) && (request.getDeliveryAddress() == null || request.getDeliveryAddress().trim().isEmpty())) {
            throw new IllegalArgumentException("選擇外送時，必須填寫外送地址！");
        }

        // 計算總金額並預先檢查庫存
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderRequest.CartItemDto itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("找不到商品編號 " + itemDto.getProductId()));
            if (product.getStock() < itemDto.getQuantity()) {
                throw new IllegalArgumentException(product.getName() + " 庫存不足！目前剩餘：" + product.getStock());
            }
            BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(itemDto.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        // 1. 建立並儲存主訂單 (Orders)
        Orders order = new Orders();
        order.setCustomerName(request.getCustomerName());
        order.setDeliveryType(dType);
        order.setDeliveryAddress("DELIVERY".equals(dType) ? request.getDeliveryAddress() : "門市自取");
        order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH");
        order.setTotalAmount(totalAmount);
        order.setOrderStatus("SUBMITTED");

        // 綁定會員帳號
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember != null) {
            order.setMemberAccount(loginMember.getAccount());
        } else {
            order.setMemberAccount("Guest");
        }

        // 設定付款狀態
        String method = order.getPaymentMethod();
        switch (method) {
            case "CASH":
                order.setPaymentStatus("UNPAID");
                break;
            case "LINE_PAY":
            case "NEWEBPAY":
                order.setPaymentStatus("PENDING");
                break;
            default:
                order.setPaymentStatus("UNPAID");
                break;
        }

        orderRepository.save(order); // 儲存主訂單以取得生成的 ID

        // 2. 迴圈扣庫存並建立訂單明細 (OrderItem)
        for (OrderRequest.CartItemDto itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId()).orElseThrow();

            // 扣庫存
            product.setStock(product.getStock() - itemDto.getQuantity());
            productRepository.save(product);

            // 建立明細
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId()); // 綁定主訂單 ID
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setQuantity(itemDto.getQuantity());
            item.setPrice(product.getPrice());
            item.setCustomization(itemDto.getCustomization());

            orderItemRepository.save(item);
        }

        return order;
    }

    // 4. 根據會員帳號查詢歷史主訂單（並帶出每筆訂單的購買明細）
    public List<Orders> getOrdersByAccount(String memberAccount) {
        List<Orders> orders = orderRepository.findByMemberAccount(memberAccount);
        for (Orders order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            order.setItems(items);
        }
        return orders;
    }

    // 5. 取消訂單並回復庫存（保留訂單與明細紀錄，狀態改為 CANCELLED）
    @Transactional
    public Orders cancelOrder(Long orderId, String requesterAccount, String requesterRole) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("找不到編號為 " + orderId + " 的訂單"));

        String status = order.getOrderStatus() != null ? order.getOrderStatus() : "SUBMITTED";

        if ("CANCELLED".equals(status)) {
            throw new IllegalArgumentException("此訂單已經取消");
        }
        if ("COMPLETED".equals(status)) {
            throw new IllegalArgumentException("已完成的訂單無法取消");
        }

        boolean isAdmin = "ADMIN".equals(requesterRole);
        if (!isAdmin) {
            if (order.getMemberAccount() == null || !order.getMemberAccount().equals(requesterAccount)) {
                throw new SecurityException("權限不足：無法取消他人的訂單");
            }
            if (!"SUBMITTED".equals(status) && !"ACCEPTED".equals(status)) {
                throw new IllegalArgumentException("訂單已進入製作或配送階段，請直接聯繫店家協助");
            }
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            if (item.getProductId() != null) {
                productRepository.findById(item.getProductId()).ifPresent(product -> {
                    product.setStock(product.getStock() + item.getQuantity());
                    productRepository.save(product);
                });
            }
        }

        order.setOrderStatus("CANCELLED");
        return orderRepository.save(order);
    }

    // 6. 付款完成更新狀態
    @Transactional
    public Orders markAsPaid(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("找不到訂單 #" + orderId));
        order.setPaymentStatus("PAID");
        return orderRepository.save(order);
    }

    // 7. 更新訂單製作/配送進度
    @Transactional
    public Orders updateOrderStatus(Long orderId, String orderStatus) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("找不到訂單 #" + orderId));
        order.setOrderStatus(orderStatus);
        return orderRepository.save(order);
    }
}