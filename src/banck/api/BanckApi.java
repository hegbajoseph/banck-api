package banck.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class BanckApi {

    static CompteService service = new CompteService();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        try {
            String envPort = System.getenv("PORT");
            if (envPort != null) port = Integer.parseInt(envPort);
        } catch (NumberFormatException ignored) {}

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/comptes", new ComptesHandler());

        // ✅ MODIFIÉ : sert le fichier index.html
        server.createContext("/", exchange -> {
            try {
                InputStream is = new FileInputStream("index.html");
                if (is == null) {
                    sendResponse(exchange, 404, "{\"erreur\":\"Page non trouvée\"}");
                    return;
                }
                byte[] bytes = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"erreur\":\"" + e.getMessage() + "\"}");
            }
        });

        server.start();
        System.out.println("✅ Serveur démarré sur le port " + port);
    }

    static class ComptesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            String[] parts = path.split("/");

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            try {
                if (method.equals("POST") && parts.length == 2) {
                    String body = readBody(exchange);
                    String nom = extractJson(body, "nom");
                    String prenom = extractJson(body, "prenom");
                    String mdp = extractJson(body, "motDePasse");
                    if (nom == null || prenom == null || mdp == null) {
                        sendResponse(exchange, 400, "{\"erreur\":\"Champs nom, prenom, motDePasse requis\"}");
                        return;
                    }
                    Compte c = service.creerCompte(nom, prenom, mdp);
                    sendResponse(exchange, 201, c.toJson());
                    return;
                }

                if (method.equals("GET") && parts.length == 2) {
                    List<Compte> comptes = service.listerComptes();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < comptes.size(); i++) {
                        sb.append(comptes.get(i).toJson());
                        if (i < comptes.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    sendResponse(exchange, 200, sb.toString());
                    return;
                }

                if (method.equals("GET") && parts.length == 3) {
                    int id = Integer.parseInt(parts[2]);
                    Compte c = service.trouverParId(id);
                    if (c == null) { sendResponse(exchange, 404, "{\"erreur\":\"Compte non trouvé\"}"); return; }
                    sendResponse(exchange, 200, c.toJson());
                    return;
                }

                if (method.equals("POST") && parts.length == 4 && parts[3].equals("depot")) {
                    int id = Integer.parseInt(parts[2]);
                    String body = readBody(exchange);
                    double montant = Double.parseDouble(extractJson(body, "montant"));
                    if (service.depot(id, montant)) {
                        Compte c = service.trouverParId(id);
                        sendResponse(exchange, 200, "{\"message\":\"Dépôt effectué\",\"nouveau_solde\":" + c.getSolde() + "}");
                    } else {
                        sendResponse(exchange, 400, "{\"erreur\":\"Dépôt impossible\"}");
                    }
                    return;
                }

                if (method.equals("POST") && parts.length == 4 && parts[3].equals("retrait")) {
                    int id = Integer.parseInt(parts[2]);
                    String body = readBody(exchange);
                    double montant = Double.parseDouble(extractJson(body, "montant"));
                    if (service.retrait(id, montant)) {
                        Compte c = service.trouverParId(id);
                        sendResponse(exchange, 200, "{\"message\":\"Retrait effectué\",\"nouveau_solde\":" + c.getSolde() + "}");
                    } else {
                        sendResponse(exchange, 400, "{\"erreur\":\"Solde insuffisant ou compte introuvable\"}");
                    }
                    return;
                }

                if (method.equals("POST") && parts.length == 4 && parts[3].equals("transfert")) {
                    int idSource = Integer.parseInt(parts[2]);
                    String body = readBody(exchange);
                    int idCible = Integer.parseInt(extractJson(body, "idCible"));
                    double montant = Double.parseDouble(extractJson(body, "montant"));
                    if (service.transfert(idSource, idCible, montant)) {
                        sendResponse(exchange, 200, "{\"message\":\"Transfert effectué\"}");
                    } else {
                        sendResponse(exchange, 400, "{\"erreur\":\"Transfert impossible (solde insuffisant ou compte introuvable)\"}");
                    }
                    return;
                }

                sendResponse(exchange, 404, "{\"erreur\":\"Route non trouvée\"}");

            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"erreur\":\"Erreur serveur : " + e.getMessage() + "\"}");
            }
        }
    }

    static void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    static String extractJson(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx);
        int start = json.indexOf("\"", colon + 1);
        if (start == -1) {
            int numStart = colon + 1;
            while (numStart < json.length() && (json.charAt(numStart) == ' ')) numStart++;
            int numEnd = numStart;
            while (numEnd < json.length() && (Character.isDigit(json.charAt(numEnd)) || json.charAt(numEnd) == '.')) numEnd++;
            return json.substring(numStart, numEnd);
        }
        int end = json.indexOf("\"", start + 1);
        return json.substring(start + 1, end);
    }
}