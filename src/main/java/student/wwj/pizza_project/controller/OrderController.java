package student.wwj.pizza_project.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import student.wwj.pizza_project.dto.OrderRequest;
import student.wwj.pizza_project.entity.Member;
import student.wwj.pizza_project.entity.Orders;
import student.wwj.pizza_project.service.OrderService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // 1. 查詢所有訂單 (GET)
    @GetMapping
    public List<Orders> getAllOrders() {
        return orderService.getAllOrders();
    }

    // 2. 根據 ID 查詢單一訂單 (GET /api/orders/{id})
    @GetMapping("/{id}")
    public Orders getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    // 3. 建立訂單 (POST /api/orders)
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        try {
            Orders createdOrder = orderService.createOrder(request);
            return ResponseEntity.ok(createdOrder);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 4. 取消訂單並回復庫存 (PATCH /api/orders/{id}/cancel)
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, HttpSession session) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) {
            return ResponseEntity.status(401).body(Map.of("message", "請先登入會員才能取消訂單！"));
        }
        String loginRole = (String) session.getAttribute("loginRole");

        try {
            Orders cancelledOrder = orderService.cancelOrder(id, loginMember.getAccount(), loginRole);
            return ResponseEntity.ok(Map.of(
                "message", "訂單 #" + id + " 已成功取消，庫存已回復！",
                "order", cancelledOrder
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 5. 查詢目前登入會員的歷史訂單 (GET /api/orders/my-orders)
    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders(HttpSession session) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) {
            return ResponseEntity.status(401).body(Map.of("message", "請先登入會員才能查看歷史訂單！"));
        }

        List<Orders> myOrders = orderService.getOrdersByAccount(loginMember.getAccount());
        return ResponseEntity.ok(myOrders);
    }

    // 6. 付款完成更新狀態 (PATCH /api/orders/{id}/pay)
    @PatchMapping("/{id}/pay")
    public ResponseEntity<?> markOrderAsPaid(@PathVariable Long id) {
        try {
            Orders updated = orderService.markAsPaid(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 7. 更新訂單進度 (PATCH /api/orders/{id}/status)
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String orderStatus = body.get("orderStatus");
            if (orderStatus == null || orderStatus.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "缺少 orderStatus 參數"));
            }
            Orders updated = orderService.updateOrderStatus(id, orderStatus);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}