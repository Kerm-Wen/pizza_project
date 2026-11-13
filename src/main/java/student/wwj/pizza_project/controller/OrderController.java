package student.wwj.pizza_project.controller;

import student.wwj.pizza_project.dto.OrderRequest;
import student.wwj.pizza_project.entity.Orders;
import student.wwj.pizza_project.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // 3. 建立訂單 (POST)
    @PostMapping
    public String createOrder(@RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }

    // 4. 取消訂單 (DELETE /api/orders/{id})
    @DeleteMapping("/{id}")
    public String cancelOrder(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }
}