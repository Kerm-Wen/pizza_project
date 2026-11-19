package student.wwj.pizza_project.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 商品名稱設定唯一約束，避免重複建檔
    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    // 商品分類: PIZZA (披薩), FRIED (炸物), DRINK (飲料)
    @Column(nullable = false)
    private String category;

    // 商品圖片網址
    @Column(name = "image_url")
    private String imageUrl;
}