package ar.org.centroblancoynegro.gestion.evaluacion.soporte;

import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Un "navegador" para los tests ocultos: cliente HTTP con su propio frasco de cookies.
 * Guarda la cookie de sesión y la de XSRF-TOKEN, y manda el token CSRF como lo hace Angular.
 * Dos instancias son dos dispositivos distintos.
 */
public final class Client {

    private final int port;
    private final CookieManager cookies = new CookieManager();
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .cookieHandler(cookies)
            .build();

    public Client(int port) {
        this.port = port;
    }

    /** GET /api/auth/me. Además deja en el frasco la cookie XSRF-TOKEN vigente. */
    public HttpResponse<String> me() {
        return send(HttpRequest.newBuilder(uri("/api/auth/me")).GET().build());
    }

    /** Pide el token CSRF como lo haría Angular y después intenta entrar. */
    public HttpResponse<String> login(String email, String password) {
        me();
        String body = "email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-XSRF-TOKEN", cookie("XSRF-TOKEN"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
    }

    private String cookie(String name) {
        return cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals(name))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "GET /api/auth/me no entregó la cookie " + name));
    }

    private URI uri(String route) {
        return URI.create("http://localhost:" + port + route);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new AssertionError("No se pudo conectar con la aplicación", e);
        }
    }
}
