package travel.core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;

/** Serves the web frontend (./web) and a small JSON API (/api/*) using the JDK's built-in HTTP server. */
public class WebServer {
    private static final Map<String, String> TYPES = Map.of(
        "html", "text/html; charset=utf-8", "css", "text/css; charset=utf-8",
        "js", "text/javascript; charset=utf-8", "svg", "image/svg+xml",
        "png", "image/png", "ico", "image/x-icon", "json", "application/json");

    public static HttpServer start(int port, Path webRoot) throws IOException {
        Path root = webRoot.toAbsolutePath().normalize();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", WebServer::api);
        server.createContext("/", ex -> staticFile(ex, root));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        return server;
    }

    private static void api(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath().replaceAll("/+$", "");
            Object result;
            if (path.equals("/api/packages") && method.equals("GET")) result = TravelService.packages();
            else if (path.equals("/api/travellers") && method.equals("GET")) result = TravelService.travellers();
            else if (path.equals("/api/travellers") && method.equals("POST")) {
                send(ex, 201, Json.write(TravelService.registerTraveller(body(ex)))); return;
            }
            else if (path.equals("/api/bookings") && method.equals("GET")) result = TravelService.bookings();
            else if (path.equals("/api/bookings") && method.equals("POST")) {
                send(ex, 201, Json.write(TravelService.book(body(ex)))); return;
            }
            else if (path.matches("/api/bookings/\\d+/cancel") && method.equals("POST")) {
                int id = Integer.parseInt(path.split("/")[3]);
                result = TravelService.cancel(id);
            }
            else throw new TravelService.ApiException(404, "Unknown endpoint.");
            send(ex, 200, Json.write(result));
        } catch (TravelService.ApiException e) {
            send(ex, e.status, Json.write(Map.of("error", e.getMessage())));
        } catch (IllegalArgumentException e) {
            send(ex, 400, Json.write(Map.of("error", "Malformed request body.")));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, Json.write(Map.of("error", "Server error: " + e.getMessage())));
        }
    }

    private static Map<String, String> body(HttpExchange ex) throws IOException {
        String s = new String(ex.getRequestBody().readNBytes(64 * 1024), StandardCharsets.UTF_8);
        return Json.parseObject(s);
    }

    private static void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private static void staticFile(HttpExchange ex, Path root) throws IOException {
        String p = ex.getRequestURI().getPath();
        if (p.equals("/")) p = "/index.html";
        Path f = root.resolve(p.substring(1)).normalize();
        if (!f.startsWith(root) || !Files.isRegularFile(f)) {
            byte[] b = "Not found".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
            return;
        }
        String name = f.getFileName().toString();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        byte[] b = Files.readAllBytes(f);
        ex.getResponseHeaders().set("Content-Type", TYPES.getOrDefault(ext, "application/octet-stream"));
        ex.sendResponseHeaders(200, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }
}
