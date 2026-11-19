package student.wwj.pizza_project.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "order_item")
@Data
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;     // 關聯的主訂單 ID
    private Long productId;   // 商品 ID
    private String productName; // 商品名稱
    private int quantity;     // 購買數量
    private BigDecimal price;     // 購買時的單價
    private String customization; // 客製化規格（如餅皮）
}