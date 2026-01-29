package org.example.model;

import java.time.Instant;
import java.util.List;

/**
 * Entité représentant une commande client (Examen K3)
 */
public class Order {
    private Integer id;
    private String reference; // Format "ORDXXXXX"
    private Instant creationDatetime;
    private List<DishOrder> dishOrders = new java.util.ArrayList<>();
    private OrderTypeEnum type;
    private OrderStatusEnum status;

    // Constructeurs
    public Order() {}

    public Order(Integer id, String reference, Instant creationDatetime,
                 OrderTypeEnum type, OrderStatusEnum status) {
        this.id = id;
        this.reference = reference;
        this.creationDatetime = creationDatetime;
        this.type = type;
        this.status = status;
    }

    // --- RÈGLE MÉTIER CRITIQUE ---
    public void setStatus(OrderStatusEnum newStatus) {
        // Si le statut actuel est déjà DELIVERED, on bloque toute modification
        if (this.status == OrderStatusEnum.DELIVERED) {
            throw new RuntimeException("ERREUR : La commande est déjà livrée (DELIVERED) et ne peut plus être modifiée.");
        }
        this.status = newStatus;
    }

    public Double getTotalAmountHT() {
        return dishOrders.stream()
                .mapToDouble(doItem -> doItem.getDish().getSellingPrice() * doItem.getQuantity())
                .sum();
    }

    public Double getTotalAmountTTC() {
        // Exemple avec une TVA à 20%
        return getTotalAmountHT() * 1.20;
    }

    // --- GETTERS & SETTERS STANDARDS ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Instant getCreationDatetime() { return creationDatetime; }
    public void setCreationDatetime(Instant creationDatetime) { this.creationDatetime = creationDatetime; }

    public List<DishOrder> getDishOrders() { return dishOrders; }
    public void setDishOrders(List<DishOrder> dishOrders) { this.dishOrders = dishOrders; }

    public OrderTypeEnum getType() { return type; }
    public void setType(OrderTypeEnum type) { this.type = type; }

    public OrderStatusEnum getStatus() { return status; }

    @Override
    public String toString() {
        return "Order{" +
                "ref='" + reference + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", totalHT=" + getTotalAmountHT() +
                ", date=" + creationDatetime +
                '}';
    }
}
