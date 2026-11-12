# 🧾 披薩點餐系統 - 訂單與庫存連動開發筆記

這份筆記記錄了在 **Spring Boot** 專案中實作「訂單處理」與「自動扣庫存」的核心架構、程式碼與測試方式。透過引入 **DTO（資料傳輸物件）** 模式，讓接收前端請求與資料庫操作職責分離，使系統架構更加標準且易於維護。

## 📂 1. 新增專案目錄與檔案結構

在原有的三層架構基礎上，新增了 `dto` 資料夾以及訂單相關的 Entity、Repository 與 Controller：

```text
pizza_project/
├── dto/
│   └── OrderRequest.java        # 資料傳輸物件（專門接收前端傳來的下單請求）
├── entity/
│   ├── Pizza.java               # 披薩實體類別
│   └── Orders.java              # 訂單實體類別（對應資料庫 orders 資料表）
├── repository/
│   ├── PizzaRepository.java     # 披薩資料存取層
│   └── OrderRepository.java     # 訂單資料存取層
└── controller/
    ├── PizzaController.java     # 披薩管理 API
    └── OrderController.java     # 訂單處理與扣庫存 API
```

---

## 💻 2. 核心程式碼彙整

### (1) 資料傳輸物件：`OrderRequest.java`
專門用來接收前端送出的下單 JSON 資料，不直接與資料庫資料表綁定。

```java
package student.wwj.pizza_project.dto;

import lombok.Data;

@Data
public class OrderRequest {
    private Long pizzaId;
    private Integer quantity;
    private String customerName;
}
```

### (2) 訂單實體層：`Orders.java`
對應 MySQL 資料庫中的 `orders` 資料表，記錄每一筆成立的訂單資訊與總金額。

```java
package student.wwj.pizza_project.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "orders")
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;             // 訂單編號

    private Long pizzaId;        // 購買的披薩 ID
    
    private String pizzaName;    // 披薩名稱（記錄當時買的名字）

    private Integer quantity;    // 購買數量

    private Double totalPrice;   // 總金額（單價 * 數量）

    private String customerName; // 訂購人姓名
}
```

### (3) 訂單資料存取層：`OrderRepository.java`
繼承 `JpaRepository` 以取得基礎的訂單新增與查詢方法。

```java
package student.wwj.pizza_project.repository;

import student.wwj.pizza_project.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Long> {
}
```

### (4) 訂單控制層：`OrderController.java`
負責處理下單流程的核心業務邏輯：
1. 驗證披薩 ID 是否存在。
2. 檢查現有庫存是否足夠。
3. 自動扣除披薩庫存並更新至資料庫。
4. 處理 `BigDecimal` 單價與數量的乘法運算（透過 `.multiply()` 與 `.doubleValue()` 轉換型態）。
5. 儲存並回傳訂單成功訊息。

```java
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
```

---

## 🌐 3. 訂單 API 端點測試一覽表

透過 **EchoAPI** 發送請求至 `http://localhost:8080`：

| 功能描述 | HTTP 方法 | 請求網址 (Endpoint) | 備註 / 傳送內容 |
| :--- | :--- | :--- | :--- |
| **查詢所有訂單** | `GET` | `/api/orders` | 回傳所有已成立訂單的 JSON 陣列 |
| **建立新訂單** | `POST` | `/api/orders` | 需在 Body (raw/JSON) 附上訂購資訊，成功後自動扣庫存 |

### 📝 下單 JSON 測試資料範例（用於 POST `/api/orders`）

```json
{
  "pizzaId": 1,
  "quantity": 2,
  "customerName": "王小明"
}
```

---

## 💡 4. 開發重點與除錯筆記

* **為什麼要獨立建立 `dto` 資料夾？**
  `entity` 專注於映射資料庫表格結構，而 `dto`（Data Transfer Object）專注於接收前端傳來的資料格式。將兩者分開能避免直接暴露資料庫結構，也讓專案架構更符合業界規範。
* **`BigDecimal` 與 `Double` 的型態轉換：**
  由於 `Pizza` 的單價 `price` 使用精確度高的 `BigDecimal`，在 Java 中不能直接使用 `*` 符號進行乘法運算，必須呼叫 `.multiply(new BigDecimal(數量))`；最後再透過 `.doubleValue()` 轉成 `Double` 存入 `Orders` 實體中。