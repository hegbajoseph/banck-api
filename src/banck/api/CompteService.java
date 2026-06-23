package banck.api;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompteService {

    private Connection getConnection() throws SQLException {
        String url = System.getenv("DATABASE_URL");
        if (url == null) {
           url = "jdbc:postgresql://dpg-d8tfdc6gvqtc73cd2260-a.frankfurt-postgres.render.com:5432/banck_db?user=banck_db_user&password=60bbD8fPz7ASNssWsDE9fG5jfXdvZ0QH&sslmode=require";
        }
        
        
        return DriverManager.getConnection(url);
    }

    private void initTable() {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.execute(
                "CREATE TABLE IF NOT EXISTS comptes (" +
                "id SERIAL PRIMARY KEY," +
                "nom VARCHAR(100) NOT NULL," +
                "prenom VARCHAR(100) NOT NULL," +
                "mot_de_passe VARCHAR(255) NOT NULL," +
                "solde DOUBLE PRECISION DEFAULT 0.0" +
                ")"
            );
        } catch (SQLException e) {
            System.err.println("Erreur init table: " + e.getMessage());
        }
    }

    public CompteService() {
        initTable();
    }

    public Compte creerCompte(String nom, String prenom, String motDePasse) {
        String sql = "INSERT INTO comptes (nom, prenom, mot_de_passe, solde) VALUES (?, ?, ?, 0.0) RETURNING id";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setString(2, prenom);
            ps.setString(3, motDePasse);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                return new Compte(id, nom, prenom, motDePasse);
            }
        } catch (SQLException e) {
            System.err.println("Erreur creerCompte: " + e.getMessage());
        }
        return null;
    }

    public List<Compte> listerComptes() {
        List<Compte> liste = new ArrayList<>();
        String sql = "SELECT id, nom, prenom, mot_de_passe, solde FROM comptes";
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Compte c = new Compte(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("mot_de_passe")
                );
                c.setSolde(rs.getDouble("solde"));
                liste.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Erreur listerComptes: " + e.getMessage());
        }
        return liste;
    }

    public Compte trouverParId(int id) {
        String sql = "SELECT id, nom, prenom, mot_de_passe, solde FROM comptes WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Compte c = new Compte(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("mot_de_passe")
                );
                c.setSolde(rs.getDouble("solde"));
                return c;
            }
        } catch (SQLException e) {
            System.err.println("Erreur trouverParId: " + e.getMessage());
        }
        return null;
    }

    public boolean depot(int id, double montant) {
        String sql = "UPDATE comptes SET solde = solde + ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, montant);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur depot: " + e.getMessage());
        }
        return false;
    }

    public boolean retrait(int id, double montant) {
        String sql = "UPDATE comptes SET solde = solde - ? WHERE id = ? AND solde >= ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, montant);
            ps.setInt(2, id);
            ps.setDouble(3, montant);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur retrait: " + e.getMessage());
        }
        return false;
    }

    public boolean transfert(int idSource, int idCible, double montant) {
        String sqlRetrait = "UPDATE comptes SET solde = solde - ? WHERE id = ? AND solde >= ?";
        String sqlDepot = "UPDATE comptes SET solde = solde + ? WHERE id = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            PreparedStatement psRetrait = conn.prepareStatement(sqlRetrait);
            psRetrait.setDouble(1, montant);
            psRetrait.setInt(2, idSource);
            psRetrait.setDouble(3, montant);
            int rows = psRetrait.executeUpdate();
            if (rows == 0) {
                conn.rollback();
                return false;
            }
            PreparedStatement psDepot = conn.prepareStatement(sqlDepot);
            psDepot.setDouble(1, montant);
            psDepot.setInt(2, idCible);
            psDepot.executeUpdate();
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erreur transfert: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) {}
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException ex) {}
        }
        return false;
    }
}
