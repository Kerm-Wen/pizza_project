package student.wwj.pizza_project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pizzaId;
    private String pizzaName;
    private Integer quantity;
    private Double totalPrice;
    private String customerName;

    // 新增：付款方式 (CASH: 現場付現, LINE_PAY: LINE Pay, NEWEBPAY: 藍新金流)
    @Column(name = "payment_method")
    private String paymentMethod;

    // 新增：付款狀態 (UNPAID: 未付款, PAID: 已付款, PENDING: 處理中/待付款)
    @Column(name = "payment_status")
    private String paymentStatus;
}