package ar.org.centroblancoynegro.gestion.evaluacion.us901;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

// CASO DE CONTROL: tiene que pasar y contar como regresion.
@SpringBootTest
class ControlRegresionTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void laBaseTieneElEsquemaDeFlyway() {
        Integer migraciones = jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history", Integer.class);
        assertThat(migraciones).isPositive();
    }
}
