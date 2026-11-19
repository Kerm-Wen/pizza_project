package student.wwj.pizza_project.service;

import student.wwj.pizza_project.dto.OrderRequest;
import student.wwj.pizza_project.entity.Member;
import student.wwj.pizza_project.entity.Orders;
import student.wwj.pizza_project.entity.OrderItem;
import student.wwj.pizza_project.entity.Product;
import student.wwj.pizza_project.repository.OrderRepository;
import student.wwj.pizza_project.repository.OrderItemRepository;
import student.wwj.pizza_project.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // 1. 查詢所有訂單（主訂單）
    public List<Orders> getAllOrders() {
        return orderRepository.findAll();
    }

    // 2. 根據 ID 查詢單一主訂單
    public Orders getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    // 3. 批次建立訂單與扣庫存（主檔 + 明細檔架構）
    @Transactional
    public String createOrder(OrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return "下單失敗：購物車是空的！";
        }

        // 外帶/外送驗證防呆
        String dType = request.getDeliveryType() != null ? request.getDeliveryType() : "TAKEOUT";
        if ("DELIVERY".equals(dType) && (request.getDeliveryAddress() == null || request.getDeliveryAddress().trim().isEmpty())) {
            return "下單失敗：選擇外送時，必須填寫外送地址！";
        }

        // 計算總金額並預先檢查庫存
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderRequest.CartItemDto itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId()).orElse(null);
            if (product == null) {
                return "下單失敗：找不到商品編號 " + itemDto.getProductId();
            }
            if (product.getStock() < itemDto.getQuantity()) {
                return "下單失敗：" + product.getName() + " 庫存不足！目前剩餘：" + product.getStock();
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

        // 綁定會員帳號
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember != null) {
            order.setMemberAccount(loginMember.getAccount());
        } else {
            order.setMemberAccount("Guest");
        }

        // 設定付款狀態
        String method = order.getPaymentMethod();
        String responseMessage = "";
        switch (method) {
            case "CASH":
                order.setPaymentStatus("UNPAID");
                responseMessage = "下單成功（現場付現）！總金額 NT$ " + totalAmount;
                break;
            case "LINE_PAY":
                order.setPaymentStatus("PENDING");
                responseMessage = "訂單已建立！正在為您導向至 LINE Pay...";
                break;
            case "NEWEBPAY":
                order.setPaymentStatus("PENDING");
                responseMessage = "訂單已建立！正在為您導向至 藍新金流...";
                break;
            default:
                order.setPaymentStatus("UNPAID");
                responseMessage = "下單成功！";
        }

        orderRepository.save(order); // 儲存主訂單以取得生成的 ID

        // 2. 迴圈扣庫存並建立訂單明細 (OrderItem)
        for (OrderRequest.CartItemDto itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId()).orElse(null);

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

        return responseMessage;
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

    // 5. 取消訂單並回復庫存（主檔-明細檔架構）
    @Transactional
    public String cancelOrder(Long orderId) {
        Orders order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return "取消失敗：找不到編號為 " + orderId + " 的訂單";
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }

        orderItemRepository.deleteAll(items);
        orderRepository.delete(order);

        return "訂單 #" + orderId + " 已成功取消，庫存已回復！";
    }
}