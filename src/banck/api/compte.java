package banck.api;

public class Compte {
    private int id;
    private String nom;
    private String prenom;
    private double solde;
    private String motDePasse;

    public Compte(int id, String nom, String prenom, String motDePasse) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.solde = 0.0;
        this.motDePasse = motDePasse;
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public double getSolde() { return solde; }
    public String getMotDePasse() { return motDePasse; }

    public void setSolde(double solde) { this.solde = solde; }

    public String toJson() {
        return String.format(
            "{\"id\":%d,\"nom\":\"%s\",\"prenom\":\"%s\",\"solde\":%.2f}",
            id, nom, prenom, solde
        );
    }
}