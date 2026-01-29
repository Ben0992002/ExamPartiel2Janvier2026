package org.example.model;

/**
 * Entité représentant la liaison entre une commande et un plat (Ligne de commande)
 */
public class DishOrder {
    private Integer id;
    private Dish dish;
    private Integer quantity;

    // Constructeurs
    public DishOrder() {}

    public DishOrder(Integer id, Dish dish, Integer quantity) {
        this.id = id;
        this.dish = dish;
        this.quantity = quantity;
    }

    // --- GETTERS & SETTERS ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Dish getDish() { return dish; }
    public void setDish(Dish dish) { this.dish = dish; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    @Override
    public String toString() {
        return "DishOrder{" +
                "dish=" + (dish != null ? dish.getName() : "null") +
                ", qty=" + quantity +
                '}';
    }
}
