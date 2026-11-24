# AGENTS.md — 美味饗宴線上點餐系統 (Pizza Ordering System)

本文件定義此專案的系統架構、資料模型、API 規格與前後端開發慣例，供開發者與 AI 協作工具（如 Gemini、Cursor、GitHub Copilot）快速理解專案脈絡並保持程式碼一致性。

---

## 1. 專案概覽 (Project Overview)

* **專案名稱**：美味饗宴線上點餐系統（披薩與速食線上點餐平台）
* **技術棧 (Tech Stack)**：
  * **後端**：Java / Spring Boot (RESTful API, Spring Data JPA)
  * **前端**：原生 HTML5 + CSS3 + Vanilla JavaScript（無前端框架，靜態檔案放置於 `src/main/resources/static/`）
  * **介面語言**：繁體中文 (`zh-TW`)

---

## 2. 目錄與核心檔案結構 (Directory Structure)

```text
src/main/
├── java/.../
│   ├── controller/
│   │   ├── OrderController.java      # 訂單建立、查詢、付款與進度狀態更新 API
│   │   ├── ProductController.java    # 商品菜單查詢 API
│   │   └── MemberController.java     # 會員註冊、登入、登出與 Session 狀態 API
│   ├── service/
│   │   └── OrderService.java         # 核心訂單邏輯、明細聚合 (Master-Detail) 與狀態流轉
│   ├── repository/                   # JPA Repositories (OrderRepository, OrderItemRepository 等)
│   └── entity/
│       ├── Orders.java               # 訂單主檔 (Master)
│       ├── OrderItem.java            # 訂單明細檔 (Detail)
│       ├── Product.java              # 商品資料
│       └── Member.java               # 會員資料
└── resources/static/
    ├── customer.html                 # 顧客端點餐頁、購物車、歷史訂單與進度條追蹤
    ├── payment.html                  # 模擬第三方金流跳轉頁 (LINE_PAY / NEWEBPAY)
    ├── admin.html                    # 店家後台管理頁（篩選訂單、檢視明細、推進製作/配送狀態）
    └── login.html                    # 會員登入頁（登出後導向頁面）
```

---

## 3. 核心資料模型與狀態機 (Domain Models & State Machine)

### 3.1 訂單主檔與明細 (`Orders` & `OrderItem`)
採用 **Master-Detail** 架構，`OrderItem` 透過 `orderId` 關聯至 `Orders`。
* 查詢訂單列表（如 `getAllOrders()`、`getOrdersByAccount()`）時，**必須**透過 `orderItemRepository.findByOrderId(order.getId())` 將明細聚合至 `order.setItems(items)`，確保前端能取得完整商品名稱 (`productName`)、數量 (`quantity`)、客製化規格 (`customization`) 與單價 (`price`)。

### 3.2 訂單狀態流轉 (`orderStatus`)
1. `SUBMITTED`：顧客送出訂單（初始狀態）
2. `ACCEPTED`：店家已接單
3. `PREPARING`：餐點製作中
4. `READY`：製作完成（若為 `TAKEOUT` 門市自取，此為最終可取餐狀態）
5. `DELIVERING`：外送員配送中（僅限 `DELIVERY` 專人外送）
6. `COMPLETED`：訂單已結案

### 3.3 付款狀態與方式 (`paymentStatus` & `paymentMethod`)
* **付款狀態 (`paymentStatus`)**：`UNPAID`（未付款）、`PAID`（已付款）
* **付款方式 (`paymentMethod`)**：
  * `CASH`：現場取餐付現（建立訂單後停留於 `customer.html` 並跳出提示）
  * `LINE_PAY` / `NEWEBPAY`：線上支付（建立訂單後自動導向 `payment.html?orderId=...&method=...&amount=...`）
* **取餐方式 (`deliveryType`)**：
  * `TAKEOUT`：門市自取（顧客端進度條顯示 5 步，不顯示 `DELIVERING`）
  * `DELIVERY`：專人外送（顧客端進度條顯示 6 步，並顯示 `deliveryAddress`）

---

## 4. 後端 API 端點規範 (API Endpoints)

| HTTP Method | Endpoint | 說明 | Request Body / 備註 |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/products` | 取得所有商品菜單 | 回傳 `List<Product>` |
| `POST` | `/api/orders` | 建立新訂單與明細 | `{ customerName, deliveryType, deliveryAddress, paymentMethod, items: [{ productId, quantity, customization }] }` |
| `GET` | `/api/orders` | 取得所有訂單（後台用） | 需包含聚合後的 `items` 明細 |
| `GET` | `/api/orders/my-orders` | 取得當前登入會員的歷史訂單 | 未登入回傳 `401` |
| `PATCH` | `/api/orders/{id}/pay` | 將指定訂單標記為已付款 | 更新 `paymentStatus` 為 `PAID` |
| `PATCH` | `/api/orders/{id}/status` | 更新指定訂單製作/配送進度 | `{ "orderStatus": "PREPARING" }` |
| `POST` | `/api/members/login` | 會員登入 | `{ account, password }` |
| `POST` | `/api/members/register` | 會員註冊 | `{ account, password, name, phone }` |
| `GET` | `/api/members/current` | 取得目前登入會員資訊 | 用於自動帶入收件人與顯示歷史訂單按鈕 |
| `POST` | `/api/members/logout` | 會員登出 | 登出後前端統一導向 `login.html` |

---

## 5. 開發與修改守則 (Coding & AI Agent Guidelines)

1. **保持單頁獨立性**：前端頁面（`customer.html`、`admin.html`、`payment.html`）各自包含完整的 HTML、CSS 與 `<script>`，修改時勿破壞既有 DOM ID 與事件綁定。
2. **前後端欄位對齊**：
   * 讀取訂單明細時，前端應相容 `order.items || order.orderItems || []`。
   * 狀態值（如 `SUBMITTED`、`PAID`、`DELIVERY`）統一使用大寫英文常數。
3. **修改範圍精準化**：
   * 進行局部功能修正時，僅修改目標函式或區塊，避免無故重寫整份檔案。
   * 涉及 Spring Boot Java 程式碼修改後，需提醒重新啟動後端服務；修改靜態 HTML 後，需提醒強制重新整理瀏覽器 (`Ctrl + F5`)。
4. **使用者體驗細節**：
   * Modal 彈跳視窗（如歷史訂單視窗）需保留右上角固定關閉按鈕 (`position: sticky`)，方便長列表捲動時隨時關閉。
   * 後台訂單列表一律將最新訂單排序在最上方 (`orders.reverse()`)。