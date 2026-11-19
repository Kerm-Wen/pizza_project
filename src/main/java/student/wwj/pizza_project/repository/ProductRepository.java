package student.wwj.pizza_project.repository;

import student.wwj.pizza_project.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // 額外提供依照分類查詢商品的便利方法
    List<Product> findByCategory(String category);
}