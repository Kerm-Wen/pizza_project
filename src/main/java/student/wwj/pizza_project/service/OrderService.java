package student.wwj.pizza_project.service;

import student.wwj.pizza_project.dto.OrderRequest;
import student.wwj.pizza_project.entity.Orders;
import student.wwj.pizza_project.entity.Pizza;
import student.wwj.pizza_project.repository.OrderRepository;
import student.wwj.pizza_project.repository.PizzaRepository;
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
    private PizzaRepository pizzaRepository;

    public List<Orders> getAllOrders() {
        return orderRepository.findAll();
    }

    public Orders getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Transactional
    public String createOrder(OrderRequest request) {
        Pizza pizza = pizzaRepository.findById(request.getPizzaId()).orElse(null);
        
        if (pizza == null) {
            return "下單失敗：找不到此編號的披薩！";
        }

        if (pizza.getStock() < request.getQuantity()) {
            return "下單失敗：庫存不足！目前剩餘庫存：" + pizza.getStock();
        }

        // 1. 扣庫存
        pizza.setStock(pizza.getStock() - request.getQuantity());
        pizzaRepository.save(pizza);

        // 2. 建立訂單
        Orders order = new Orders();
        order.setPizzaId(pizza.getId());
        order.setPizzaName(pizza.getName());
        order.setQuantity(request.getQuantity());
        order.setTotalPrice(pizza.getPrice().multiply(new BigDecimal(request.getQuantity())).doubleValue());
        order.setCustomerName(request.getCustomerName());
        
        // 3. 設定付款方式與初始狀態（修正：改用 getPaymentMethod()）
        String method = request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH";
        order.setPaymentMethod(method);

        String responseMessage = "";

        switch (method) {
            case "CASH":
                order.setPaymentStatus("UNPAID"); // 修正：移除多餘的參數名稱提示
                responseMessage = "下單成功（現場付現）！感謝 " + request.getCustomerName() + " 購買 " + request.getQuantity() + " 個 " + pizza.getName();
                break;
            case "LINE_PAY":
                order.setPaymentStatus("PENDING"); // 修正：移除多餘的參數名稱提示
                responseMessage = "訂單已建立！正在為您導向至 LINE Pay 結帳頁面...";
                break;
            case "NEWEBPAY":
                order.setPaymentStatus("PENDING"); // 修正：移除多餘的參數名稱提示
                responseMessage = "訂單已建立！正在為您導向至 藍新金流 (NewebPay) 結帳頁面...";
                break;
            default:
                order.setPaymentStatus("UNPAID"); // 修正：移除多餘的參數名稱提示
                responseMessage = "下單成功！";
        }
        
        orderRepository.save(order);
        return responseMessage;
    }

    @Transactional
    public String cancelOrder(Long id) {
        Orders order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            return "取消失敗：找不到編號為 " + id + " 的訂單！";
        }

        Pizza pizza = pizzaRepository.findById(order.getPizzaId()).orElse(null);
        if (pizza != null) {
            pizza.setStock(pizza.getStock() + order.getQuantity());
            pizzaRepository.save(pizza);
        }

        orderRepository.deleteById(id);

        return "訂單編號 " + id + " 已成功取消，且商品庫存已自動回補！";
    }
}