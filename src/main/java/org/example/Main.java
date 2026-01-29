package org.example;

import org.example.model.*;
import org.example.repository.DataRetriever;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Main {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("RESTAU-GESTION : TD4 (Stocks) & EXAMEN K3 (Commandes)");
        System.out.println("=".repeat(80));

        DataRetriever dataRetriever = new DataRetriever();

        try {
            // ==========================================
            // PARTIE 1 : TEST DU STOCK (TD4)
            // ==========================================
            System.out.println("\n📦 ÉTAPE 1 : Récupération de la Laitue (ID: 1)...");
            Ingredient laitue = dataRetriever.findIngredientById(1);

            System.out.println("✓ Ingrédient : " + laitue.getName());
            System.out.println("  Nombre de mouvements enregistrés : " + laitue.getStockMovementList().size());

            // Date de test pour le TD4 : 2024-01-06 12:00
            Instant testTime = LocalDateTime.of(2024, 1, 6, 12, 0).toInstant(ZoneOffset.UTC);
            StockValue stockAtT = laitue.getStockValueAt(testTime);

            System.out.println("\n🔢 Calcul du stock au 2024-01-06 12:00 :");
            System.out.println("  --------------------------------------------------");

            double entrees = laitue.getStockMovementList().stream()
                    .filter(m -> !m.getCreationDatetime().isAfter(testTime))
                    .filter(m -> m.getType() == MovementTypeEnum.IN)
                    .mapToDouble(m -> m.getValue().getQuantity()).sum();

            double sorties = laitue.getStockMovementList().stream()
                    .filter(m -> !m.getCreationDatetime().isAfter(testTime))
                    .filter(m -> m.getType() == MovementTypeEnum.OUT)
                    .mapToDouble(m -> m.getValue().getQuantity()).sum();

            System.out.println("  Somme Entrées (IN)  : " + entrees + " KG");
            System.out.println("  Somme Sorties (OUT) : " + sorties + " KG");
            System.out.println("  Stock Résultant     : " + stockAtT.getQuantity() + " " + stockAtT.getUnit());

            // Vérification du résultat attendu (4.8 KG)
            double expected = 4.8;
            if (Math.abs(stockAtT.getQuantity() - expected) < 0.01) {
                System.out.println("  ✅ RÉSULTAT CORRECT (Attendu: 4.8)");
            } else {
                System.err.println("  ❌ ERREUR (Attendu: 4.8, Obtenu: " + stockAtT.getQuantity() + ")");
            }

            // ==========================================
            // PARTIE 2 : TEST DES COMMANDES (EXAMEN K3)
            // ==========================================
            System.out.println("\n🚀 ÉTAPE 2 : Test de sauvegarde d'une commande...");

            Order newOrder = new Order();
            newOrder.setReference("ORD-" + System.currentTimeMillis());
            newOrder.setType(OrderTypeEnum.TAKE_AWAY);
            newOrder.setStatus(OrderStatusEnum.CREATED);
            newOrder.setCreationDatetime(Instant.now());

            // Sauvegarde en base de données
            dataRetriever.saveOrder(newOrder);
            System.out.println("✅ Commande enregistrée avec succès en base de données !");

            // Test de la sécurité d'immuabilité du statut
            System.out.println("\n🛡️ ÉTAPE 3 : Test de sécurité du statut DELIVERED...");
            newOrder.setStatus(OrderStatusEnum.DELIVERED);
            System.out.println("  Statut actuel : DELIVERED");

            try {
                System.out.println("  Tentative de retour au statut READY (devrait échouer)...");
                newOrder.setStatus(OrderStatusEnum.READY);
                System.err.println("  ❌ ÉCHEC : La sécurité n'a pas bloqué le changement !");
            } catch (RuntimeException e) {
                System.out.println("  ✅ SÉCURITÉ OK : " + e.getMessage());
            }

            System.out.println("\n" + "=".repeat(80));
            System.out.println("FIN DES TESTS AVEC SUCCÈS");

        } catch (Exception e) {
            System.err.println("\n💥 ERREUR CRITIQUE DURANT LES TESTS :");
            System.err.println("Message : " + e.getMessage());
            e.printStackTrace();
        }
    }
}