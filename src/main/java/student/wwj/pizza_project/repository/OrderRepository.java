package student.wwj.pizza_project.repository;

import student.wwj.pizza_project.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List; // 記得引入 List

@Repository
public interface OrderRepository extends JpaRepository<Orders, Long> {

    // --- 新增：根據會員帳號查詢所有歷史訂單 ---
    List<Orders> findByMemberAccount(String memberAccount);
    
}