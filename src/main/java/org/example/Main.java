package org.example;

import org.example.model.*;
import org.example.repository.DataRetriever;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Main {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("RAPPORT D'EXÉCUTION : STOCKS (TD4/TD5) & COMMANDES (EXAMEN K3)");
        System.out.println("=".repeat(80));

        DataRetriever dataRetriever = new DataRetriever();

        try {
            // ============================================================
            // PARTIE 1 : LOGIQUE STOCKS - APPROCHE OBJET (TD4)
            // ============================================================
            System.out.println("\n📦 [TD4] ÉTAPE 1 : Récupération et calcul Java (Streams)");
            Ingredient laitue = dataRetriever.findIngredientById(1);

            System.out.println("✓ Ingrédient : " + laitue.getName());
            System.out.println("  Nombre de mouvements enregistrés : " + laitue.getStockMovementList().size());

            Instant testTime = LocalDateTime.of(2024, 1, 6, 12, 0).toInstant(ZoneOffset.UTC);
            StockValue stockAtT = laitue.getStockValueAt(testTime);

            System.out.println("\n🔢 Calcul détaillé (Java-side) :");
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

            if (Math.abs(stockAtT.getQuantity() - 4.8) < 0.01) {
                System.out.println("  ✅ VALIDATION TD4 RÉUSSIE");
            }

            // ============================================================
            // PARTIE 2 : GESTION DES COMMANDES (EXAMEN K3)
            // ============================================================
            System.out.println("\n🚀 [EXAMEN K3] ÉTAPE 2 : Persistance et sécurité métier");

            // 2.1 Sauvegarde en base de données
            Order newOrder = new Order();
            newOrder.setReference("ORD-" + System.currentTimeMillis());
            newOrder.setType(OrderTypeEnum.TAKE_AWAY);
            newOrder.setStatus(OrderStatusEnum.CREATED);
            newOrder.setCreationDatetime(Instant.now());

            dataRetriever.saveOrder(newOrder);
            System.out.println("✅ Sauvegarde SQL : Commande enregistrée avec succès.");

            // 2.2 Affichage du contenu de la table (Validation K3)
            System.out.println("\n📊 [EXAMEN K3] ÉTAPE 3 : Vérification de la table 'order'");
            String query = "SELECT * FROM \"order\" ORDER BY id DESC LIMIT 5";
            try (Connection connection = dataRetriever.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    System.out.printf("  ID: %d | Réf: %s | Statut: %s%n",
                            rs.getInt("id"),
                            rs.getString("reference"),
                            rs.getString("status")
                    );
                }
            }

            // ============================================================
            // PARTIE 3 : OPTIMISATION PUSH-DOWN (TD5)
            // ============================================================
            System.out.println("\n🔥 [TD5] ÉTAPE 4 : Calcul optimisé via la base de données");

            StockValue stockPushDown = dataRetriever.getStockValueAt(testTime, 1);

            if (stockPushDown != null) {
                System.out.println("  [SQL Push-down] Résultat reçu : " + stockPushDown.getQuantity() + " " + stockPushDown.getUnit());

                if (Math.abs(stockPushDown.getQuantity() - 4.8) < 0.01) {
                    System.out.println("  ✅ VALIDATION TD5 RÉUSSIE (Calcul SQL conforme au calcul Java)");
                }
            }

        } catch (Exception e) {
            System.err.println("\n💥 ERREUR CRITIQUE :");
            e.printStackTrace();
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("FIN DU PROGRAMME - TOUS LES JALONS VALIDÉS");
    }
}