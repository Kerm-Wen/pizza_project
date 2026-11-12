package student.wwj.pizza_project.controller;

import student.wwj.pizza_project.dto.OrderRequest;
import student.wwj.pizza_project.entity.Orders;
import student.wwj.pizza_project.entity.Pizza;
import student.wwj.pizza_project.repository.OrderRepository;
import student.wwj.pizza_project.repository.PizzaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PizzaRepository pizzaRepository;

    // 1. 查詢所有訂單 (GET)
    @GetMapping
    public List<Orders> getAllOrders() {
        return orderRepository.findAll();
    }

    // 2. 建立訂單並自動扣庫存 (POST)
    @PostMapping
    public String createOrder(@RequestBody OrderRequest request) {
        // 先找出客人想買的披薩
        Pizza pizza = pizzaRepository.findById(request.getPizzaId()).orElse(null);
        
        if (pizza == null) {
            return "下單失敗：找不到此編號的披薩！";
        }

        // 檢查庫存是否夠
        if (pizza.getStock() < request.getQuantity()) {
            return "下單失敗：庫存不足！目前剩餘庫存：" + pizza.getStock();
        }

        // 開始扣庫存
        pizza.setStock(pizza.getStock() - request.getQuantity());
        pizzaRepository.save(pizza); // 把更新後的庫存寫回資料庫

        // 建立訂單紀錄
        Orders order = new Orders();
        order.setPizzaId(pizza.getId());
        order.setPizzaName(pizza.getName());
        order.setQuantity(request.getQuantity());
        
        // 總金額計算：BigDecimal 相乘後轉為 Double 存入
        order.setTotalPrice(pizza.getPrice().multiply(new BigDecimal(request.getQuantity())).doubleValue());
        
        order.setCustomerName(request.getCustomerName());
        
        orderRepository.save(order);

        return "下單成功！感謝 " + request.getCustomerName() + " 購買 " + request.getQuantity() + " 個 " + pizza.getName();
    }
}