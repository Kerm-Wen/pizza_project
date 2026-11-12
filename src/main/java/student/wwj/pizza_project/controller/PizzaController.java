package student.wwj.pizza_project.controller;

import student.wwj.pizza_project.entity.Pizza;
import student.wwj.pizza_project.repository.PizzaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pizzas")
public class PizzaController {

    @Autowired
    private PizzaRepository pizzaRepository;

    // 1. 查詢所有披薩 (GET)
    @GetMapping
    public List<Pizza> getAllPizzas() {
        return pizzaRepository.findAll();
    }

    // 2. 新增披薩 (POST)
    @PostMapping
    public Pizza createPizza(@RequestBody Pizza pizza) {
        return pizzaRepository.save(pizza);
    }

    // 3. 依 ID 查詢單一披薩 (GET)
    @GetMapping("/{id}")
    public Pizza getPizzaById(@PathVariable Long id) {
        return pizzaRepository.findById(id).orElse(null);
    }

    // 4. 查詢所有「上架中」的披薩 (GET)
    @GetMapping("/available")
    public List<Pizza> getAvailablePizzas() {
        return pizzaRepository.findByIsAvailableTrue();
    }

    // 5. 更新披薩資料 (PUT)
    @PutMapping("/{id}")
    public Pizza updatePizza(@PathVariable Long id, @RequestBody Pizza pizzaDetails) {
        Pizza pizza = pizzaRepository.findById(id).orElse(null);
        if (pizza != null) {
            pizza.setName(pizzaDetails.getName());
            pizza.setPrice(pizzaDetails.getPrice());
            pizza.setDescription(pizzaDetails.getDescription());
            pizza.setIsAvailable(pizzaDetails.getIsAvailable());
            pizza.setStock(pizzaDetails.getStock());
            return pizzaRepository.save(pizza);
        }
        return null;
    }

    // 6. 刪除披薩資料 (DELETE)
    @DeleteMapping("/{id}")
    public String deletePizza(@PathVariable Long id) {
        if (pizzaRepository.existsById(id)) {
            pizzaRepository.deleteById(id);
            return "成功刪除 ID 為 " + id + " 的披薩！";
        }
        return "找不到 ID 為 " + id + " 的披薩，刪除失敗。";
    }
}