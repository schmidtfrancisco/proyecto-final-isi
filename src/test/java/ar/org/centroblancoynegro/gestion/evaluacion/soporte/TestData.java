package ar.org.centroblancoynegro.gestion.evaluacion.soporte;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Carga datos de prueba con SQL directo, sin pasar por el código de la IA.
 */
public final class TestData {

    /** Contraseña de todos los usuarios de prueba. */
    public static final String PASSWORD = "Clave-Segura-123";

    /** CLAVE cifrada con BCrypt, en el formato del contrato ({bcrypt} + hash). */
    public static final String PASSWORD_HASH =
            "{bcrypt}$2a$10$utEMktWOCB/yxsALlTcfYenojspAPWvMV4.79qBKU45ck0RMJqkgq";

    private final JdbcTemplate jdbc;

    public TestData(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Crea una persona llamada Ana Pérez, su usuario y un rol. Devuelve el id del usuario.
     *
     * @param passwordHash  contraseña ya cifrada, o null para una cuenta sin activar
     * @param role          un valor del enum user_role, por ejemplo "STAFF"
     */
    public long createUser(String email, String passwordHash, boolean active, String role) {
        Long personId = jdbc.queryForObject(
                "INSERT INTO people (first_name, last_name) VALUES ('Ana', 'Pérez') RETURNING id",
                Long.class);
        Long userId = jdbc.queryForObject(
                "INSERT INTO users (person_id, email, password_hash, is_active) "
                        + "VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, personId, email, passwordHash, active);
        jdbc.update(
                "INSERT INTO user_roles (user_id, role) VALUES (?, CAST(? AS user_role))",
                userId, role);
        return userId;
    }
}
