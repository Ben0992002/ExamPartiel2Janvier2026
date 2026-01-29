package org.example.model;

import java.util.ArrayList;
import java.util.List;

public class Dish {
    private Integer id;
    private String name;
    private Double sellingPrice;

    // AJOUT : La liste des ingrédients qui composent le plat (la recette)
    private List<DishIngredient> dishIngredients = new ArrayList<>();

    public Dish() {}

    public Dish(Integer id, String name, Double sellingPrice) {
        this.id = id;
        this.name = name;
        this.sellingPrice = sellingPrice;
    }

    // --- GETTERS & SETTERS ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(Double sellingPrice) { this.sellingPrice = sellingPrice; }

    // AJOUT : Getter pour la liste des ingrédients
    public List<DishIngredient> getDishIngredients() {
        return dishIngredients;
    }

    public void setDishIngredients(List<DishIngredient> dishIngredients) {
        this.dishIngredients = dishIngredients;
    }

    @Override
    public String toString() {
        return "Dish{" +
                "name='" + name + '\'' +
                ", ingredientsCount=" + dishIngredients.size() +
                '}';
    }
}