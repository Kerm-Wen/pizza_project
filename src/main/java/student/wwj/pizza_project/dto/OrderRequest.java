package student.wwj.pizza_project.dto;

import lombok.Data;

@Data
public class OrderRequest {
    private Long pizzaId;
    private Integer quantity;
    private String customerName;
}