package org.example.repository;

import java.time.Instant;
import org.example.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp; // Pour le "Timestamp.from(t)"
import java.time.Instant;

/**
 * Repository pour gérer les opérations CRUD sur les ingrédients et leurs stocks
 */
public class DataRetriever {

    /**
     * Trouve un ingrédient par son ID avec tous ses mouvements de stock
     * 
     * @param id l'identifiant de l'ingrédient
     * @return l'ingrédient avec ses mouvements de stock
     * @throws RuntimeException si l'ingrédient n'existe pas
     */
    public Ingredient findIngredientById(Integer id) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                    SELECT id, name, price, category 
                    FROM ingredient 
                    WHERE id = ?
                    """);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Ingredient ingredient = new Ingredient();
                ingredient.setId(resultSet.getInt("id"));
                ingredient.setName(resultSet.getString("name"));
                ingredient.setPrice(resultSet.getDouble("price"));
                ingredient.setCategory(CategoryEnum.valueOf(resultSet.getString("category")));

                // Charger les mouvements de stock associés
                ingredient.setStockMovementList(findStockMovementsByIngredientId(id));

                dbConnection.closeConnection(connection);
                return ingredient;
            }

            dbConnection.closeConnection(connection);
            throw new RuntimeException("Ingredient not found with ID: " + id);

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de l'ingrédient", e);
        }
    }

    /**
     * Trouve tous les mouvements de stock d'un ingrédient
     * 
     * @param ingredientId l'identifiant de l'ingrédient
     * @return la liste des mouvements de stock ordonnés par date
     */
    private List<StockMovement> findStockMovementsByIngredientId(Integer ingredientId) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        List<StockMovement> movements = new ArrayList<>();

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                    SELECT id, quantity, unit, type, creation_datetime 
                    FROM stock_movement 
                    WHERE id_ingredient = ?
                    ORDER BY creation_datetime ASC
                    """);
            preparedStatement.setInt(1, ingredientId);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                StockMovement movement = new StockMovement();
                movement.setId(resultSet.getInt("id"));
                movement.setType(MovementTypeEnum.valueOf(resultSet.getString("type")));
                movement.setCreationDatetime(resultSet.getTimestamp("creation_datetime").toInstant());

                StockValue value = new StockValue();
                value.setQuantity(resultSet.getDouble("quantity"));
                value.setUnit(Unit.valueOf(resultSet.getString("unit")));

                movement.setValue(value);
                movements.add(movement);
            }

            dbConnection.closeConnection(connection);
            return movements;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche des mouvements de stock", e);
        }
    }

    /**
     * Sauvegarde un ingrédient avec ses mouvements de stock
     * 
     * Règles importantes :
     * - Si l'ingrédient n'a pas d'ID : création (INSERT)
     * - Si l'ingrédient a un ID : mise à jour (UPDATE)
     * - Pour les mouvements de stock :
     *   * Si mouvement.id existe : ON CONFLICT DO NOTHING (pas de modification)
     *   * Si mouvement.id == null : création d'un nouveau mouvement
     * - Les mouvements ne peuvent PAS être supprimés
     * 
     * @param toSave l'ingrédient à sauvegarder
     * @return l'ingrédient sauvegardé complet (rechargé depuis la BD)
     * @throws SQLException en cas d'erreur SQL
     */
    public Ingredient saveIngredient(Ingredient toSave) throws SQLException {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        try {
            connection.setAutoCommit(false);

            // 1. Sauvegarder ou mettre à jour l'ingrédient
            Integer ingredientId;
            if (toSave.getId() == null) {
                ingredientId = insertIngredient(connection, toSave);
            } else {
                ingredientId = updateIngredient(connection, toSave);
            }

            // 2. Sauvegarder les mouvements de stock
            if (toSave.getStockMovementList() != null && !toSave.getStockMovementList().isEmpty()) {
                saveStockMovements(connection, ingredientId, toSave.getStockMovementList());
            }

            connection.commit();

            // 3. Recharger l'ingrédient complet depuis la base de données
            dbConnection.closeConnection(connection);
            return findIngredientById(ingredientId);

        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Erreur lors de la sauvegarde de l'ingrédient", e);
        } finally {
            dbConnection.closeConnection(connection);
        }
    }

    /**
     * Insère un nouvel ingrédient dans la base de données
     */
    private Integer insertIngredient(Connection connection, Ingredient ingredient) throws SQLException {
        String sql = """
            INSERT INTO ingredient (id, name, category, price)
            VALUES (?, ?, ?::ingredient_category, ?)
            RETURNING id
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (ingredient.getId() != null) {
                ps.setInt(1, ingredient.getId());
            } else {
                ps.setInt(1, getNextSerialValue(connection, "ingredient", "id"));
            }
            ps.setString(2, ingredient.getName());
            ps.setString(3, ingredient.getCategory().name());
            ps.setDouble(4, ingredient.getPrice());

            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Met à jour un ingrédient existant dans la base de données
     */
    private Integer updateIngredient(Connection connection, Ingredient ingredient) throws SQLException {
        String sql = """
            UPDATE ingredient 
            SET name = ?, category = ?::ingredient_category, price = ?
            WHERE id = ?
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ingredient.getName());
            ps.setString(2, ingredient.getCategory().name());
            ps.setDouble(3, ingredient.getPrice());
            ps.setInt(4, ingredient.getId());

            ps.executeUpdate();
            return ingredient.getId();
        }
    }

    /**
     * Sauvegarde les mouvements de stock
     * 
     * Règle TD4 : Si un mouvement a déjà un ID, on utilise ON CONFLICT DO NOTHING
     * Cela signifie que si l'ID existe déjà, PostgreSQL ignore l'insertion
     * (pas d'erreur, pas de mise à jour)
     */
    private void saveStockMovements(Connection connection, Integer ingredientId,
                                    List<StockMovement> movements) throws SQLException {
        String sql = """
            INSERT INTO stock_movement (id, id_ingredient, quantity, unit, type, creation_datetime)
            VALUES (?, ?, ?, ?::unit, ?::movement_type, ?)
            ON CONFLICT (id) DO NOTHING
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (StockMovement movement : movements) {
                // Si le mouvement a un ID, on l'utilise (avec ON CONFLICT DO NOTHING)
                // Sinon, on génère un nouvel ID
                if (movement.getId() != null) {
                    ps.setInt(1, movement.getId());
                } else {
                    ps.setInt(1, getNextSerialValue(connection, "stock_movement", "id"));
                }

                ps.setInt(2, ingredientId);
                ps.setDouble(3, movement.getValue().getQuantity());
                ps.setString(4, movement.getValue().getUnit().name());
                ps.setString(5, movement.getType().name());
                ps.setTimestamp(6, Timestamp.from(movement.getCreationDatetime()));

                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Trouve tous les ingrédients avec leurs mouvements de stock
     */
    public List<Ingredient> findAllIngredients() {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        List<Ingredient> ingredients = new ArrayList<>();

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                    SELECT id, name, price, category 
                    FROM ingredient 
                    ORDER BY id
                    """);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Ingredient ingredient = new Ingredient();
                ingredient.setId(resultSet.getInt("id"));
                ingredient.setName(resultSet.getString("name"));
                ingredient.setPrice(resultSet.getDouble("price"));
                ingredient.setCategory(CategoryEnum.valueOf(resultSet.getString("category")));
                ingredient.setStockMovementList(findStockMovementsByIngredientId(ingredient.getId()));

                ingredients.add(ingredient);
            }

            dbConnection.closeConnection(connection);
            return ingredients;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche des ingrédients", e);
        }
    }

    // ========== MÉTHODES UTILITAIRES POUR GÉRER LES SÉQUENCES POSTGRESQL ==========

    /**
     * Récupère le nom de la séquence associée à une colonne
     */
    private String getSerialSequenceName(Connection conn, String tableName, String columnName)
            throws SQLException {
        String sql = "SELECT pg_get_serial_sequence(?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    /**
     * Récupère la prochaine valeur de la séquence
     */
    private int getNextSerialValue(Connection conn, String tableName, String columnName)
            throws SQLException {
        String sequenceName = getSerialSequenceName(conn, tableName, columnName);
        if (sequenceName == null) {
            throw new IllegalArgumentException(
                    "No sequence found for " + tableName + "." + columnName
            );
        }

        // Synchroniser la séquence avec la valeur maximale actuelle
        updateSequenceNextValue(conn, tableName, columnName, sequenceName);

        // Récupérer la prochaine valeur
        String nextValSql = "SELECT nextval(?)";
        try (PreparedStatement ps = conn.prepareStatement(nextValSql)) {
            ps.setString(1, sequenceName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void updateSequenceNextValue(Connection conn, String tableName,
                                        String columnName, String sequenceName) throws SQLException {
        String setValSql = String.format(
                "SELECT setval('%s', (SELECT COALESCE(MAX(%s), 0) FROM %s))",
                sequenceName, columnName, tableName
        );
        try (PreparedStatement ps = conn.prepareStatement(setValSql)) {
            ps.executeQuery();
        }
    }

    public Order saveOrder(Order order) throws SQLException {
        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false); // Transaction pour la sécurité

            // 1. LIEN TD4 : Validation rigoureuse des stocks
            for (DishOrder item : order.getDishOrders()) {
                for (DishIngredient di : item.getDish().getDishIngredients()) {
                    // On utilise ton code du TD4 pour calculer le stock actuel
                    StockValue currentStock = di.getIngredient().getStockValueAt(Instant.now());
                    double needed = di.getQuantity() * item.getQuantity();

                    if (currentStock == null || currentStock.getQuantity() < needed) {
                        conn.rollback();
                        throw new RuntimeException("Stock insuffisant pour : " + di.getIngredient().getName());
                    }
                }
            }

            // 2. SAUVEGARDE (INSERT ou UPDATE)
            String sql = (order.getId() == null) ?
                    "INSERT INTO \"order\" (reference, type, status, creation_datetime) VALUES (?, ?::order_type, ?::order_status, ?) RETURNING id" :
                    "UPDATE \"order\" SET type = ?::order_type, status = ?::order_status WHERE id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (order.getId() == null) {
                    ps.setString(1, order.getReference());
                    ps.setString(2, order.getType().name());
                    ps.setString(3, order.getStatus().name());
                    ps.setTimestamp(4, Timestamp.from(order.getCreationDatetime()));
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) order.setId(rs.getInt(1));
                } else {
                    ps.setString(1, order.getType().name());
                    ps.setString(2, order.getStatus().name());
                    ps.setInt(3, order.getId());
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return order;
        }
    }

        public Connection getConnection() {
            return new DBConnection().getConnection();
        }

        /**
         * TD5 : Calcul du stock directement par la base de données (Push-down processing)
         * On ne charge plus la liste en Java, on demande le résultat à SQL.
         */
        public StockValue getStockValueAt(Instant t, Integer ingredientId) {
            // 1. La requête SQL (Le Push-down)
            // On transforme les 'OUT' en négatif et on additionne tout
            String query = """
            SELECT 
                unit, 
                SUM(CASE WHEN type = 'OUT' THEN -quantity ELSE quantity END) as actual_stock
            FROM stock_movement
            WHERE id_ingredient = ? AND creation_datetime <= ?
            GROUP BY unit
        """;

            // 2. Connexion et préparation (Try-with-resources pour fermer auto la connexion)
            try (Connection conn = new DBConnection().getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {

                // 3. Remplissage des paramètres (les points d'interrogation ?)
                ps.setInt(1, ingredientId);
                ps.setTimestamp(2, java.sql.Timestamp.from(t)); // Conversion Instant -> SQL Timestamp

                // 4. Exécution et lecture du résultat
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        StockValue sv = new StockValue();
                        sv.setQuantity(rs.getDouble("actual_stock"));
                        sv.setUnit(Unit.valueOf(rs.getString("unit")));
                        return sv;
                    }
                }
            } catch (SQLException e) {
                // Si la base de données n'est pas accessible
                throw new RuntimeException("Erreur TD5 lors du calcul SQL", e);
            }

            // Si l'ingrédient n'a aucun mouvement, on retourne null
            return null;
        }

    }





