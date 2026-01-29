package org.example.model;

public class DishIngredient {
    private Ingredient ingredient;
    private Double quantity; // ex: 0.5

    public DishIngredient() {}

    public DishIngredient(Ingredient ingredient, Double quantity) {
        this.ingredient = ingredient;
        this.quantity = quantity;
    }

    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
}
