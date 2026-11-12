package student.wwj.pizza_project.repository;

import student.wwj.pizza_project.entity.Pizza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PizzaRepository extends JpaRepository<Pizza, Long> {
    
    // 自訂查詢：只找出所有上架中的披薩
    List<Pizza> findByIsAvailableTrue();
}