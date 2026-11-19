package student.wwj.pizza_project.dto;

import java.util.List;

public class OrderRequest {
    // 1. 訂單共用資訊
    private String customerName;
    private String deliveryType;
    private String deliveryAddress;
    private String paymentMethod;
    
    // 2. 購物車商品明細列表（支援一次傳送多個商品）
    private List<CartItemDto> items;

    // 內部類別：用來接收購物車裡的每一個商品品項
    public static class CartItemDto {
        private Long productId;
        private Integer quantity;
        private String customization;

        // Getters and Setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public String getCustomization() { return customization; }
        public void setCustomization(String customization) { this.customization = customization; }
    }

    // Orders Level Getters and Setters
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }
}