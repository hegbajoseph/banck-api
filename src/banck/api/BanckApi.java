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
        int port = 7070;
        try {
            String envPort = System.getenv("PORT");
            if (envPort != null) port = Integer.parseInt(envPort);
        } catch (NumberFormatException ignored) {}

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/comptes", new ComptesHandler());

        server.createContext("/swagger", exchange -> {
            try {
                InputStream is = new FileInputStream("swagger.html");
                byte[] bytes = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"erreur\":\"" + e.getMessage() + "\"}");
            }
        });

        server.createContext("/swagger.yaml", exchange -> {
            try {
                InputStream is = new FileInputStream("swagger.yaml");
                byte[] bytes = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/yaml");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"erreur\":\"" + e.getMessage() + "\"}");
            }
        });

        server.createContext("/", exchange -> {
            try {
                InputStream is = new FileInputStream("index.html");
                byte[] bytes = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"erreur\":\"" + e.getMessage() + "\"}");
            }
        });

        server.createContext("/Bank-API-Integration_big-1.webp", exchange -> {
            try {
                InputStream is = new FileInputStream("Bank-API-Integration_big-1.webp");
                byte[] bytes = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "image/webp");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            } catch (Exception e) {
                sendResponse(exchange, 404, "{\"erreur\":\"Image non trouvee\"}");
            }
        });

        server.start();
        System.out.println("Serveur demarre sur le port " + port);
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
                    if (c == null) { sendResponse(exchange, 404, "{\"erreur\":\"Compte non trouve\"}"); return; }
                    sendResponse(exchange, 200, c.toJson());
                    return;
                }

                if (method.equals("POST") && parts.length == 4 && parts[3].equals("depot")) {
                    int id = Integer.parseInt(parts[2]);
                    String body = readBody(exchange);
                    double montant = Double.parseDouble(extractJson(body, "montant"));
                    if (service.depot(id, montant)) {
                        Compte c = service.trouverParId(id);
                        sendResponse(exchange, 200, String.format(java.util.Locale.US,
                            "{\"message\":\"Depot effectue\",\"nouveau_solde\":%.2f}", c.getSolde()));
                    } else {
                        sendResponse(exchange, 400, "{\"erreur\":\"Depot impossible\"}");
                    }
                    return;
                }

                if (method.equals("POST") && parts.length == 4 && parts[3].equals("retrait")) {
                    int id = Integer.parseInt(parts[2]);
                    String body = readBody(exchange);
                    double montant = Double.parseDouble(extractJson(body, "montant"));
                    if (service.retrait(id, montant)) {
                        Compte c = service.trouverParId(id);
                        sendResponse(exchange, 200, String.format(java.util.Locale.US,
                            "{\"message\":\"Retrait effectue\",\"nouveau_solde\":%.2f}", c.getSolde()));
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
                        sendResponse(exchange, 200, "{\"message\":\"Transfert effectue\"}");
                    } else {
                        sendResponse(exchange, 400, "{\"erreur\":\"Transfert impossible (solde insuffisant ou compte introuvable)\"}");
                    }
                    return;
                }

                sendResponse(exchange, 404, "{\"erreur\":\"Route non trouvee\"}");

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
        try {
            String search = "\"" + key + "\"";
            int idx = json.indexOf(search);
            if (idx == -1) return null;
            int colon = json.indexOf(":", idx + search.length());
            if (colon == -1) return null;
            int pos = colon + 1;
            while (pos < json.length() && json.charAt(pos) == ' ') pos++;
            if (pos >= json.length()) return null;
            char first = json.charAt(pos);
            if (first == '"') {
                StringBuilder sb = new StringBuilder();
                pos++;
                while (pos < json.length()) {
                    char c = json.charAt(pos);
                    if (c == '\\' && pos + 1 < json.length()) {
                        pos++;
                        char escaped = json.charAt(pos);
                        switch (escaped) {
                            case '"': sb.append('"'); break;
                            case '\\': sb.append('\\'); break;
                            case 'n': sb.append('\n'); break;
                            case 't': sb.append('\t'); break;
                            default: sb.append(escaped);
                        }
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c);
                    }
                    pos++;
                }
                return sb.toString();
            } else {
                int numEnd = pos;
                while (numEnd < json.length() &&
                       (Character.isDigit(json.charAt(numEnd)) ||
                        json.charAt(numEnd) == '.' ||
                        json.charAt(numEnd) == '-')) {
                    numEnd++;
                }
                return json.substring(pos, numEnd);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
