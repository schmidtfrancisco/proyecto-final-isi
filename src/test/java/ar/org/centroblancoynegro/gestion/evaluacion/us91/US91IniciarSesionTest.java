package ar.org.centroblancoynegro.gestion.evaluacion.us91;

import static ar.org.centroblancoynegro.gestion.evaluacion.soporte.TestData.PASSWORD;
import static ar.org.centroblancoynegro.gestion.evaluacion.soporte.TestData.PASSWORD_HASH;
import static org.assertj.core.api.Assertions.assertThat;

import ar.org.centroblancoynegro.gestion.evaluacion.soporte.Client;
import ar.org.centroblancoynegro.gestion.evaluacion.soporte.TestData;
import ar.org.centroblancoynegro.gestion.evaluacion.soporte.Errors;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tests ocultos de la US-91 · Iniciar sesión.
 *
 * Son de caja negra: preparan los datos con SQL directo y hablan con la
 * aplicación solo por HTTP, como lo haría el navegador.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("US-91 · Iniciar sesión")
class US91IniciarSesionTest {

    /** Margen para comparar la hora de la app con la de este test. */
    private static final Duration MARGIN = Duration.ofSeconds(2);

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    TestData data;

    @BeforeEach
    void setUpDatabase() {
        jdbc.execute("TRUNCATE user_roles, users, people RESTART IDENTITY CASCADE");
        data = new TestData(jdbc);
    }

    // ------------------------------------------------------------------
    //  Entrar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Ingreso válido: entra y queda con la sesión iniciada")
    void ingresoValido() {
        long id = data.createUser("ana.perez@gmail.com", PASSWORD_HASH, true, "STAFF");
        Client browser = new Client(port);

        HttpResponse<String> login = browser.login("ana.perez@gmail.com", PASSWORD);
        assertThat(login.statusCode()).isEqualTo(204);

        HttpResponse<String> me = browser.me();
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(((Number) JsonPath.read(me.body(), "$.id")).longValue()).isEqualTo(id);
        assertThat((String) JsonPath.read(me.body(), "$.email")).isEqualTo("ana.perez@gmail.com");
        assertThat((String) JsonPath.read(me.body(), "$.firstName")).isEqualTo("Ana");
        assertThat((String) JsonPath.read(me.body(), "$.lastName")).isEqualTo("Pérez");
        assertThat((List<String>) JsonPath.read(me.body(), "$.roles")).containsExactly("STAFF");
    }

    @Test
    @DisplayName("Correo con otras mayúsculas: entra igual")
    void correoConOtrasMayusculas() {
        data.createUser("ana.perez@gmail.com", PASSWORD_HASH, true, "STAFF");
        Client browser = new Client(port);

        HttpResponse<String> login = browser.login("Ana.Perez@Gmail.com", PASSWORD);

        assertThat(login.statusCode()).isEqualTo(204);
        assertThat(browser.me().statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("Último ingreso: queda registrada la fecha y hora del ingreso")
    void ultimoIngreso() {
        long id = data.createUser("ana.perez@gmail.com", PASSWORD_HASH, true, "STAFF");
        Client browser = new Client(port);

        Instant before = Instant.now().minus(MARGIN);
        HttpResponse<String> login = browser.login("ana.perez@gmail.com", PASSWORD);
        Instant after = Instant.now().plus(MARGIN);

        assertThat(login.statusCode()).isEqualTo(204);
        OffsetDateTime lastLogin = jdbc.queryForObject(
                "SELECT last_login_at FROM users WHERE id = ?", OffsetDateTime.class, id);
        assertThat(lastLogin).isNotNull();
        assertThat(lastLogin.toInstant()).isBetween(before, after);
    }

    @Test
    @DisplayName("Varias sesiones: entrar desde otro dispositivo no cierra la primera sesión")
    void variasSesiones() {
        data.createUser("ana.perez@gmail.com", PASSWORD_HASH, true, "STAFF");
        Client computer = new Client(port);
        Client phone = new Client(port);

        assertThat(computer.login("ana.perez@gmail.com", PASSWORD).statusCode()).isEqualTo(204);
        assertThat(phone.login("ana.perez@gmail.com", PASSWORD).statusCode()).isEqualTo(204);

        assertThat(computer.me().statusCode()).isEqualTo(200);
        assertThat(phone.me().statusCode()).isEqualTo(200);
    }

    // ------------------------------------------------------------------
    //  Cuándo no entra
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Contraseña incorrecta: no entra")
    void contrasenaIncorrecta() {
        data.createUser("ana.perez@gmail.com", PASSWORD_HASH, true, "STAFF");
        Client browser = new Client(port);

        HttpResponse<String> login = browser.login("ana.perez@gmail.com", "Otra-Clave-999");

        Errors.verify(login, 401, "BAD_CREDENTIALS");
        assertThat(browser.me().statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Correo inexistente: mismo error que con contraseña incorrecta")
    void correoInexistente() {
        Client browser = new Client(port);

        HttpResponse<String> login = browser.login("nadie@gmail.com", PASSWORD);

        Errors.verify(login, 401, "BAD_CREDENTIALS");
        assertThat(browser.me().statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Cuenta sin activar: no entra, con el mismo error")
    void cuentaSinActivar() {
        data.createUser("ana.perez@gmail.com", null, true, "STAFF");
        Client browser = new Client(port);

        HttpResponse<String> login = browser.login("ana.perez@gmail.com", PASSWORD);

        Errors.verify(login, 401, "BAD_CREDENTIALS");
        assertThat(browser.me().statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Usuario desactivado: no entra y se informa que está desactivado")
    void usuarioDesactivado() {
        data.createUser("ana.perez@gmail.com", PASSWORD_HASH, false, "STAFF");
        Client browser = new Client(port);

        HttpResponse<String> login = browser.login("ana.perez@gmail.com", PASSWORD);

        Errors.verify(login, 401, "USER_DISABLED");
        assertThat(browser.me().statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Desactivado con contraseña incorrecta: error genérico")
    void desactivadoConContrasenaIncorrecta() {
        data.createUser("ana.perez@gmail.com", PASSWORD_HASH, false, "STAFF");
        Client navegador = new Client(port);

        HttpResponse<String> login = navegador.login("ana.perez@gmail.com", "Otra-Clave-999");

        Errors.verify(login, 401, "BAD_CREDENTIALS");
        assertThat(navegador.me().statusCode()).isEqualTo(401);
    }

    // ------------------------------------------------------------------
    //  Lo que no toca
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un intento fallido no cambia la fecha del último ingreso")
    void intentoFallidoNoEsIngreso() {
        long id = data.createUser("ana.perez@gmail.com", PASSWORD_HASH, true, "STAFF");
        OffsetDateTime previousTimestamp = OffsetDateTime.parse("2026-01-01T13:00:00Z");
        jdbc.update("UPDATE users SET last_login_at = ? WHERE id = ?", previousTimestamp, id);
        Client browser = new Client(port);

        HttpResponse<String> login = browser.login("ana.perez@gmail.com", "Otra-Clave-999");

        assertThat(login.statusCode()).isEqualTo(401);
        OffsetDateTime lastLogin = jdbc.queryForObject(
                "SELECT last_login_at FROM users WHERE id = ?", OffsetDateTime.class, id);
        assertThat(lastLogin.toInstant()).isEqualTo(previousTimestamp.toInstant());
    }
}
