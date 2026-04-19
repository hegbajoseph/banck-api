package banck.api;

import java.util.ArrayList;
import java.util.List;

public class CompteService {
    private List<Compte> comptes = new ArrayList<>();
    private int nextId = 1;

    public Compte creerCompte(String nom, String prenom, String motDePasse) {
        Compte c = new Compte(nextId++, nom, prenom, motDePasse);
        comptes.add(c);
        return c;
    }

    public List<Compte> listerComptes() {
        return comptes;
    }

    public Compte trouverParId(int id) {
        return comptes.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }

    public boolean depot(int id, double montant) {
        Compte c = trouverParId(id);
        if (c == null || montant <= 0) return false;
        c.setSolde(c.getSolde() + montant);
        return true;
    }

    public boolean retrait(int id, double montant) {
        Compte c = trouverParId(id);
        if (c == null || montant <= 0 || c.getSolde() < montant) return false;
        c.setSolde(c.getSolde() - montant);
        return true;
    }

    public boolean transfert(int idSource, int idCible, double montant) {
        if (!retrait(idSource, montant)) return false;
        depot(idCible, montant);
        return true;
    }
}
