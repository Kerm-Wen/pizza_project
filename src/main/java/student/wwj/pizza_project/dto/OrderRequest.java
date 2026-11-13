package student.wwj.pizza_project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequest {
    private Long pizzaId;
    private Integer quantity;
    private String customerName;
    
    // 新增：讓前端帶入付款方式
    private String paymentMethod; // "CASH", "LINE_PAY", "NEWEBPAY"
}