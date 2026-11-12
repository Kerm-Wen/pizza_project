package student.wwj.pizza_project.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "orders")
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;          // 訂單編號

    private Long pizzaId;     // 購買的披薩 ID
    
    private String pizzaName; // 披薩名稱（記錄當時買的名字）

    private Integer quantity; // 購買數量

    private Double totalPrice; // 總金額（單價 * 數量）

    private String customerName; // 訂購人姓名
}